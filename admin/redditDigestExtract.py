#!/usr/bin/python3

# Program to extract reddit posts for a given sub.
# Arguments - subreddit name
# Date to extract - yyyy-mm

import sys
import os
import glob
import re
import json
import datetime
from redditRequest import redditRequest

def processPost(postData):
  global lastPostId, lastYearNo, lastMonthNo, lastDayNo

  lastPostId = postData["name"]     # the lastPostId is what is used to get the next page of posts.

  removedFlag = postData["removed"]
  if removedFlag:
    print(f"Skipping removed post {lastPostId} from u/{author} - title: {titleText}")

  postTimestamp = datetime.datetime.fromtimestamp(postData["created"])
  lastYearNo = postTimestamp.year
  lastMonthNo = postTimestamp.month
  lastDayNo = postTimestamp.day


def processPosts(jsonData, targetYear, targetMonth):
    # It seems that there is a limit of about 1000 entries when retrieving history
    # so, we need to check that we actually got some data
    # If we did then process it, if not, just skip over this and
    # indicate no need to continue processing.
    # Refer to this post:
    #   https://www.reddit.com/r/redditdev/comments/9vmpqt/hello_is_there_a_limit_to_how_far_back_in_a_subs/
  data = jsonData["data"]
  posts = data["children"]
  if len(posts) > 0:
    for post in posts:
      postKind = post["kind"]
      if (postKind == "t3"):
        processPost(post["data"])
      else:
        print("\nDEBUG **** not processing post kind: {}\n".format(post["kind"]))
  else:
    print("No posts in reply, so quitting")
  return ((lastYearNo == targetYear and lastMonthNo >= targetMonth) or lastYearNo > targetYear) and len(posts) > 0
  #print("post count: {}".format(len(posts)))


def usage(msg = None):
  if msg != None:
    print(msg)
  print(f"{os.path.basename(sys.argv[0])} subreddit_name extract_date run_id")
  print("where:")
  print("  subreddit_name  is the name of the subbredit to extract from.")
  print("                  do not include the r/. For example arduino, not r/arduino.")
  print("  extract_date    is the date for which to extract data for.")
  print("                  format yyyy-mm. For example 2022-09 for September 2022.")
  print("  run_id          a unique identifier for this run. Important when multiple")
  print("                  extracts are required to get a complete set of data from reddit.")
  sys.exit(1)



if len(sys.argv) == 4:      # Check that we have the subreddit name and the date (yyyy-mm)
  subredditName = sys.argv[1]
  extractDate = sys.argv[2]
  runId = sys.argv[3]
else:
  usage()

extractPattern = re.compile('(\d{4,4})-(\d{2,2})')
extractMatcher = extractPattern.match(extractDate)

if extractMatcher:
  targetYearStr = extractMatcher.group(1)
  targetMonthStr = extractMatcher.group(2)
  targetYear = int(targetYearStr)
  targetMonth = int(targetMonthStr)
  if (targetMonth < 1 or targetMonth > 12):
    usage("The month is not valid, it should be a two digit integer between 01 and 12")
else:
  usage("Invalid parameter for extract_date: {}")


rootDir = "reddit"
responseFilePath = os.path.join(rootDir, "digest")
responseFileNameTemplate = "digestResp-" +subredditName + "-" + targetYearStr + "-" + targetMonthStr + "-" + runId + "-{:02}.json"

print("Operating parameters:")
print(f"  subreddit: {subredditName}")
print(f"  target year: {targetYear}, month: {targetMonth} ({targetMonthStr})")
print(f"  runId: {runId}")
print(f"responseFileNameTemplate: {responseFileNameTemplate}")

# Check to see if there are any existing files that will match this extract pattern.
# If there are, print an error, print the files and exit.
# This is to prevent any accidental overwriting of the files.
checkExtractFilesPattern = os.path.join(responseFilePath, responseFileNameTemplate.replace("{:02}", "*"))
existingExtractFileList = glob.glob(checkExtractFilesPattern)
if (len(existingExtractFileList) > 0):
  print("\n*** Error:\nSome extract files already exist for this pattern:")
  for f in existingExtractFileList:
    print(f"  {f}")
  print(f"Check file pattern: {checkExtractFilesPattern}")
  print("Cleanup with:")
  print(f"    rm {checkExtractFilesPattern}")
  sys.exit(1)

# Following is the post that is used to retrive a list of new posts.
requestText = f"https://oauth.reddit.com/r/{subredditName}/new"
requestParameters = {"limit" : 100 }
lastPostId = "n/a"
pageNo = 0
continueProcessing = True
while continueProcessing:
  print(f"requesting page {pageNo}, lastPostId: {lastPostId}")
  responseFileName = responseFileNameTemplate.format(pageNo)
  responseFileNamePath = os.path.join(responseFilePath, responseFileName)
  jsonStr = redditRequest(requestText, requestParameters, responseFileNamePath, verbose=False)
  print(f"Response file written {responseFileNamePath}")

  jsonData = json.loads(jsonStr)
  continueProcessing = processPosts(jsonData, targetYear, targetMonth)
  # Set the starting point for the next page request.
  requestParameters ["after"] = lastPostId
  pageNo = pageNo + 1
  print (f"DEBUG: **** Last post id: {lastPostId}  Last post date: {lastYearNo}-{lastMonthNo}-{lastDayNo}, continue processing: {continueProcessing}\n")


#print (f"Data from {responseFileName}")
print (f"Last post id: {lastPostId}  ")
