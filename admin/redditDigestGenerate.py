#!/usr/bin/python3

#### Generate the reddit monthly digest for a given subreddit and target date.
#
# This script assumes that the top level subreddit feed has been extracted into
# one or more JSON files. These are the JSON pages returned from the reddit API
# in response to the "new" posts sorting order.
#
# Note that the reddit API appears to limit the number of historical records to
# about 1000. So, if there are likely to be close to - or more than - 1,000 posts in
# a month, this script can process multiple extracts from multiple days during a single
# month.
#
# The script will merge multiple extracts by using the "observedDate", which is taken
# from the creation timestamp of the JSON file. In otherwords, it will consider all of
# the extracted data and use the latest version of any given post. This also allows
# removed posts to be handled as such posts do appear in the extract, but are
# annotated as "removed".
#
# By: G. McCall
#     September-2022
#     V 1.00.00.00


import sys
import os
import re
import glob
import json
import datetime

from dataclasses import dataclass

# A class to hold the details extracted from a post
# in the various dictionaries maintained by this script
# the sole purpose of this class is to name the fields
# being stored (for easier and less error prone access).
@dataclass
class Post:
  name: str
  author: str
  postTitle: str
  linkFlairText: str
  relativeUrlText: str
  absoluteUrlText: str
  createdUtc: float
  created: float
  ups: int
  downs: int
  score: int
  upvoteRatio : float
  awardsCount: int
  numComments: int
  removed: bool
  observedTs: float       # The date that this post was observed.



# number of flair types
# key: flair text (link_flair_text)
# value: count of posts using that flair.
flairTypeCnt = {}

# number of posts by user
# key: user name (author)
# value: count of posts by that user.
userPostCnt = {}

# Dictionary of posts extracted from response files.
# key: name (an id of the form t3_x19ogo)
# value instance of Post object [name, author, title, link_flair_text, permalink, url, created_utc, ups, downs, score, upvote_ratio, num_comments }
postsDict = {}

# Look what I made Posts grouped by a score.
# Key: score
# value: list of posts having this score.
scoredLWIMPostsDict = {}

# Moderator choice(s) grouped by a score.
# Key: score
# value: list of posts having this score.
scoredModsChoiceDict = {}

# ALl posts grouped by a score.
# Key: score
# value: list of posts having this score.
topPostsDict = {}



MAX_TITLE_LEN = 40



# Generic function to increment a key in a Dictionary
# or add an initial value if the key is new.
def incrementDictCnt(theDict, key, inc = 1):
  if key in theDict:
    
    theDict[key] = theDict[key] + inc
  else:
    theDict[key] = inc
  



# Increment a flair count - allowing for the potential
# that the flair might be Null/None.
def incrementFlairCnt(key, inc = 1):
  if key == None:
    key = "no flair"
  incrementDictCnt(flairTypeCnt, key, inc)



# increment a User Post count - allowing for the potential
# that the user ID might be Null/None (which should never
# happen).
def incrementUserPostCnt(key, inc = 1):
  if key == None:
    key = "no uid"
  incrementDictCnt(userPostCnt, key)



# Generic function to organise posts by a numerical value (e.g. a score).
# the structure created by this function is a dictionary where the key is
# the numeric value (i.e. the score). If the score is new, a list with a
# single element being the post is placed into the dictionary.
# If the score has previously been seen, then the post is appended to the
# existing list of posts at that score point.
def sortedByScore(dictionary, score, post):
  if post.score in dictionary:
    dictionary[post.score].append(post)
  else:
    dictionary[post.score] = [ post ]


# escapeMarkdown in text
# escapes reddit markdown characters in a string.
# It seems like the escapes do not quite work properly in a markdown table
# so for now, we will just remove them completely.
def escapeMarkdown(str):
  #return  re.sub(r"([\:\[\]\(\)\|\#])", r"\\\1", str)
  #return  re.sub(r"([\[\]\|\#])", r"\\\1", str)
  
  # Attempt to deal with italicisation of _name_
  matcher = re.match(r"_(\w*)_", str)
  if (matcher != None):
    str = f"\\\\_{matcher.group(1)}\\\\_"
  
  return  re.sub(r"([\[\]\|\#])", r"", str)

# Summarise the posts in various fashions.
# When this function is called, we have loaded all of the "current" posts
# into the postsDict dictionary.
# This function scans that dictionary and allocates the posts to the various
# other dictionaries from which the final report (the markdown output) is
# generated.
def summarisePosts():
  for postId in postsDict:
    post = postsDict[postId]
    
    # Allocate the post if it has one of the flairs that we are interested in.
    if post.linkFlairText == "Look what I made!":
      sortedByScore(scoredLWIMPostsDict, post.score, post)
    elif post.linkFlairText != None and "Mod's Choice!" in post.linkFlairText:
      print(f"******    Adding post {post.name} with flair {post.linkFlairText} to mods choice")
      sortedByScore(scoredModsChoiceDict, post.score, post)

    # Place the post into the correct score tier for the final determination of
    # the top 10 (or so) posts
    sortedByScore(topPostsDict, post.score, post)



def postTableMarkdownHeader(mdFile, header = None):
  if header != None:
    mdFile.write(f"\n\n## {header}\n\n")

  mdFile.write("|Title|Author|Score|Comments|\n")
  mdFile.write("|---|---|--:|--:|\n")



# Generic function that outputs a post to the markdown file as a table row.
def postToMarkdownTableRow(mdFile, post):
  titleText = post.postTitle
  if len(titleText) > MAX_TITLE_LEN:
    titleText = titleText[0: MAX_TITLE_LEN - 1] + "..."
  mdFile.write(f"|[{escapeMarkdown(titleText)}]({post.relativeUrlText})|u/{escapeMarkdown(post.author)}|{post.score:,}|{post.numComments:,}|\n")



# It is assumed that the summarisePosts function has done what it needs to do
# to allocate the posts to the various reporting structures.
# This function then processes the reporting dictionary structures and
# outputs the markdown ready for posting.
def generateDigestMarkdown(rootDir, markdownFileName, subredditName, targetDate):
  
  print(f"\nWriting markdown")
  # Open the final output file name for writing (previous copies will be overwritten).
  with open(markdownFileName, "w") as mdFile:
    
    
    # Output the overall heading for the post - ideally this would end up in the
    # post title rather than the actual post.
    mdFile.write(f"# r/{subredditName} Monthly digest for {targetDate}\n")
    

    # output the header (if any)
    headerFileName = f"digest-{subredditName}-{targetDate}-header.md"
    headerFilePath = os.path.join(rootDir, headerFileName)
    if (os.path.exists(headerFilePath)):
      with open(headerFilePath, "r") as headerFile:
        print(f"Writing Header from {headerFileName}")
        headerText = headerFile.read()
        mdFile.write(f"\n\n{headerText}\n\n")
    else:
      print(f"No header for {subredditName}-{targetDate} ({headerFileName})")


    mdFile.write(f"\n\n## Arduino Wiki and Other Resources\n");
    mdFile.write(f"\nDon't forget to check out our [wiki](https://www.reddit.com/r/arduino/wiki/index/)\n");
    mdFile.write(f"for up to date guides, FAQ, milestones, glossary and more.\n\n")
    mdFile.write(f"You can find our [wiki](https://www.reddit.com/r/arduino/wiki/index/) at the top of the r/Arduino\n")
    mdFile.write(f"posts feed and in our \"tools/reference\" sidebar panel.\n")
    mdFile.write(f"The sidebar also has a selection of links to additional useful information and tools.\n\n")


    # Output the moderators choice if any.
    if (len(scoredModsChoiceDict) > 0):
      postTableMarkdownHeader(mdFile, "Moderator's Choice" if len(scoredModsChoiceDict) == 1 else "Moderator's Choices")

      postCnt = 0
      for postScore in sorted(scoredModsChoiceDict, reverse = True):
        for post in scoredModsChoiceDict[postScore]:
          if not post.removed:
            postCnt = postCnt + 1
            postToMarkdownTableRow(mdFile, post)


    # Output the "top 10 (or so) posts" heading and posts table.
    postTableMarkdownHeader(mdFile, "Top Posts")
    postCnt = 0
    for postScore in sorted(topPostsDict, reverse = True):
      for post in topPostsDict[postScore]:
        if not post.removed:
          postCnt = postCnt + 1
          postToMarkdownTableRow(mdFile, post)
      if postCnt >= 10:
        break

    # Output the "look what I made" heading and posts table.
    postTableMarkdownHeader(mdFile, "Look what I made posts")

    postCount = 0
    for score in sorted(scoredLWIMPostsDict, reverse=True):
      for post in scoredLWIMPostsDict[score]:

        if not post.removed:
          postCount = postCount + 1
          postToMarkdownTableRow(mdFile, post)
        #print(f"|[{titleText}]({post.relativeUrlText})|u/{post.author}|{post.score}|{post.numComments}|")
    mdFile.write(f"\nTotal: {postCount} posts\n\n")


    # output the flair summary if any have been used.
    if len(flairTypeCnt) > 0:
      #print ("\nFlair type counts:  ")
      mdFile.write(f"\n\n## Summary of Post types:\n\n")
      mdFile.write(f"|Flair|Count|\n")
      mdFile.write(f"|---|--:|\n")
      tmpDict = {}
      for key in flairTypeCnt.keys():
        tmpDict[re.sub(":.*?:", "", key).strip()] = flairTypeCnt[key]

      for key in sorted(tmpDict.keys()):
        #print(f"{flairTypeCnt[key]:10} {key}  ")
        
        mdFile.write(f"|{key}|{tmpDict[key]:,}|\n")
    
    # Finally write a summary message showing the total number of posts that
    # have been loaded.
    mdFile.write(f"\n\nTotal: {len(postsDict)} posts in {extractDateStr}\n\n")

    # output the footer (if any)
    footerFileName = f"digest-{subredditName}-{targetDate}-footer.md"
    footerFilePath = os.path.join(rootDir, footerFileName)
    if (os.path.exists(footerFilePath)):
      with open(footerFilePath, "r") as footerFile:
        print(f"Writing Footer from {footerFileName}")
        footerText = footerFile.read()
        mdFile.write(f"\n\n{footerText}\n\n")
    else:
      print(f"No footer for {subredditName}-{targetDate} ({footerFileName})")


# Extract the various details from a post supplied as a dictionary constructed
# from the JSON extracted from the reddit data.
# 
# Useful fields are transported from the post dictionary into an instance of a Post
# class. From here, the post is recorded in the postsDict dictionary, removed from
# the postsDict dictionary, placed into the postsDict dictionary or simply discarded.
def processPost(postData, targetYear, targetMonth, observationTs):
  global yearNo, monthNo, dayNo

                                        # Extract the year, month and day from the created timestamp.
  createdTimestamp = postData["created"]  # Created timestamp (seems to be UTC)
  createdUtcTimestamp = postData["created_utc"]   # Created timestamp (UTC)
  postTimestamp = datetime.datetime.fromtimestamp(createdUtcTimestamp)
  yearNo = postTimestamp.year
  monthNo = postTimestamp.month
  dayNo = postTimestamp.day


  # Is the post in the target date?
  if yearNo == targetYear and monthNo == targetMonth:
    # Extract the various fields.
    author = postData["author"]           # author's user ID.
    postId = postData["name"]             # the postId is what is used to get the next page of posts.
    titleText = postData["title"]         # Title of the post

    removedFlag = False
    if "removed"in postData:
      postData["removed"]     # Has this post been removed?

    selfText = postData["selftext"]       # The body of the post.
    commentCnt = postData["num_comments"] # Number of comments attached to the post.
    upVotes = postData["ups"]             # Number of upvotes (seems to actually be the net total of ups-downs if the result is +ve)
    downVotes = postData["downs"]         # Number of upvotes (seems to actually be the net total of ups-downs if the result is -ve)
    score = postData["score"]             # upvotes - downvotes.
    upvoteRatio = postData["upvote_ratio"]  # The upvote ratio as reported by reddit
    commentCnt = postData["num_comments"]   # Number of comments attached to post as reported by reddit
    awardsCount = postData["total_awards_received"]   # Number of awards given to the post.

    relativeLinkText = postData["permalink"]          # A relative URL path to the post
    absoluteLinkText = postData["url"]                # A fully qualified URL to the post
    linkFlairText = postData["link_flair_text"]       # The link flair.

    post = Post(postId, author, titleText, linkFlairText, relativeLinkText, absoluteLinkText, 
                createdUtcTimestamp, createdTimestamp,
                upVotes, downVotes, score, upvoteRatio, awardsCount, commentCnt,
                removedFlag, observationTs)

    # Have we previously seen this post?
    if postId in postsDict:
      # Is this new post later than the one we currently have in the posts dictionary?
      if postsDict[postId].observedTs < post.observedTs:
        # Yes, determine how to deal with it.
        if removedFlag:
          # Remove the post and decrement the stats we have been maintaining
          print (f"removing post: {postId}")
          del postsDict[postId]
          incrementUserPostCnt(post.author, -1)
          incrementFlairCnt(post.linkFlairText, -1)
        else:
          # Otherwise, replace the recorded post with this new version of it.
          # adjust the flair counts just in case the flair was changed.
          #print(f"Replacing {postsDict[postId].observedTs} with {post.observedTs} (as per timstamp)")
          incrementFlairCnt(post.linkFlairText)
          incrementFlairCnt(postsDict[postId].linkFlairText, -1)
          postsDict[postId] = post
      else:
        pass        # Nothing to do if the new post is not later than the current version.
    else:
      # we didn't see this post before, so, simply record it.
      postsDict[postId] = post
      incrementUserPostCnt(post.author)
      incrementFlairCnt(post.linkFlairText)



# Given a dictionary loaded from a reddit response file (containing a list of
# posts), step through the posts one by one and process them.
def processPosts(jsonData, targetYear, targetMonth, observationTs):
  data = jsonData["data"]
  posts = data["children"]
  for post in posts:
    postKind = post["kind"]
    if (postKind == "t3"):
      processPost(post["data"], targetYear, targetMonth, observationTs)
    else:
      print("\nDEBUG **** not processing post kind: {}\n".format(post["kind"]))



# Output usage information preceeded by an optional message.
# This function never returns.
def usage(msg = None):
  if msg != None:
    print(msg)
  print(f"{os.path.basename(sys.argv[0])} subreddit_name extract_date")
  print("where:")
  print("  subreddit_name  is the name of the subbredit to extract from.")
  print("                  do not include the r/. For example arduino, not r/arduino.")
  print("  extract_date    is the date for which to extract data for.")
  print("                  format yyyy-mm. For example 2022-09 for September 2022.")
  sys.exit(1)


# main

# Do we have enough command line arguments?
if len(sys.argv) == 3:      # Check that we have the subreddit name and the date (yyyy-mm)
  subredditName = sys.argv[1]
  extractDateStr = sys.argv[2]
else:
  usage()

# Check the date for reasonableness - looks like yyyy-mm
# and the month is 1 to 12.
extractPattern = re.compile('(\d{4,4})-(\d{2,2})')
extractMatcher = extractPattern.match(extractDateStr)

if extractMatcher:
  targetYearStr = extractMatcher.group(1)
  targetMonthStr = extractMatcher.group(2)
  targetYear = int(targetYearStr)
  targetMonth = int(targetMonthStr)
  if (targetMonth < 1 or targetMonth > 12):
    usage("The month is not valid, it should be a two digit integer between 01 and 12")
else:
  usage("Invalid parameter for extract_date: {}")



# Establish some working parameters.
# Basically the files it processes are:
# inputs:  $rootDir/digest/digestResp-$subName-*-yyyy-mm-*.json
# markdown output: $rootDir/digest-$subName-yyyy-mm.md
rootDir = "reddit"
responseFilePath = os.path.join(rootDir, "digest")
responseFileNamePattern = "digestResp-" + subredditName + "-" + targetYearStr + "-" + targetMonthStr + "-*-*.json"

markdownFileName = os.path.join(rootDir, f"digest-{subredditName}-{targetYearStr}-{targetMonthStr}.md")

print("Operating parameters:")
print(f"  subreddit: {subredditName}")
print(f"  target year: {targetYear}, month: {targetMonth} ({targetMonthStr})")
print(f"  file pattern: {responseFileNamePattern}")
print(f"digest file name: {markdownFileName}")


# Determine list of reddit response files matching the target date.
fileList = glob.glob(os.path.join(responseFilePath, responseFileNamePattern))

if len(fileList) == 0:
  print("No matching files")
  sys.exit(0)


# Process each of the identified files one by one.
# This is the first step that loads the most recent version of a post into
# a dictionary keyed by the post Id.
for fileName in fileList:
  print(f"processing: {fileName}")
  # Use the file created date/time as the observation date.
  # The observation date is used to determine which is the later version of a post
  # if we see it one more than one occasion.
  observationTs = os.path.getmtime(fileName)
  with open(fileName, "r") as responseFile:
    jsonData = json.load(responseFile)
    processPosts(jsonData, targetYear, targetMonth, observationTs)

# Summarise the loaded posts into various "reporting structures".
summarisePosts()

# Finally, output the markdown file using the "reporting structures" created
# from the previous step.
generateDigestMarkdown(rootDir, markdownFileName, subredditName, extractDateStr)

print (f"Markdown: {markdownFileName}")
print (f"Number of posts: {len(postsDict)}  ")
