#!/usr/bin/python

import sys
import os
import json
import re
from redditRequest import redditRequest


# Consider using this curl
# curl -A "testClient/1.0" "https://www.reddit.com/r/arduino/comments/zlwtq4/half_a_million_subscribers_enroll_here_to_receive.json" >response.json


hasImageRegEx = r"(https://preview.redd.it/\w+\.(jpeg|png))|(https://wokwi.com/projects)"
hasImagePattern = re.compile(hasImageRegEx, re.IGNORECASE)

userActivity = {}
userImageCount = {}

def outputUserActivity():
  totalActivity = 0
  print("\nUser activity:")
  for userId in sorted(userActivity):
    totalActivity = totalActivity + userActivity[userId]
    print ("{:5}, {}, image Count: {}".format(userActivity[userId], userId, userImageCount[userId]))
    
  print("total commentators: {} with {} comments".format(len(userActivity), totalActivity))
  


def processRepliesHelper(elem, indent):
  global userActivity
  data = elem["data"]
  
  for child in data["children"]:
    kind = child["kind"]
    if (kind == "t1"):
      #print("kind: {}".format(child["kind"]))
      childData = child["data"]
      author = childData["author"]
      replies = childData["replies"]
      upVotes = childData["ups"]
      downVotes = childData["downs"]
      removed = False
      if "removed" in childData:
        removed = childData["removed"]
      body = childData["body"]

      if (not removed):
        if(author in userActivity):
          userActivity[author] = userActivity[author] + 1
        else:
          userActivity[author] = 1

        # initialise the image count for a discovered user.
        if(author not in userImageCount):
          userImageCount[author] = 0

        # count 1 if the user's post looks like it contains an image.
        if hasImagePattern.search(body) != None:
          userImageCount[author] = userImageCount[author] + 1


      print("{}author: {}{}, ups: {}, downs {}, replies: {}, {}".format(indent, author, " (deleted)" if removed else "", upVotes, downVotes, len(replies), "Image" if hasImagePattern.search(body) else ""))
      if (len(replies) > 0):
        processRepliesHelper (childData["replies"], indent + "  ")



def processReplies(jsonData):
  print("Comment structure:")
  cnt = 0
  for elem in jsonData:
    #print (type(elem))
    #print("elem len {}".format(len(elem)))
    #print(f"Processing element {cnt}")
    cnt = cnt + 1
    processRepliesHelper(elem, "")






if (len(sys.argv) != 2):
  print("Please specify the URL to the milestone post to be examined")
  print("example:")
  print("  https://www.reddit.com/r/arduino/comments/zlwtq4/half_a_million_subscribers_enroll_here_to_receivehttps://www.reddit.com/r/arduino/comments/zlwtq4/half_a_million_subscribers_enroll_here_to_receive")
  sys.exit(1)


postURL = sys.argv[1]

queryStringLocn = postURL.find("?")
if (queryStringLocn != -1):
  postURL = postURL[:queryStringLocn]

if (postURL.endswith("/")):
  postURL = postURL[:-1]

if (not "oauth" in postURL):
  postURL = postURL + ".json"

print ("Processing: " + postURL)




#requestParameters = {"limit" : 100 }
print(f"requesting page {postURL}")
requestText = postURL
requestParameters = {"limit" : 2000 }
responseFileName = "responseMilestone.json"
responseFilePath = "/tmp"
responseFileNamePath = os.path.join(responseFilePath, responseFileName)


continueProcessing = True
while continueProcessing:
  jsonStr = redditRequest(requestText, requestParameters, responseFileNamePath, sendToken = ("oauth" in requestText))
  #print(f"Response file written {responseFileNamePath}")
  jsonData = json.loads(jsonStr)
  continueProcessing = processReplies(jsonData)


outputUserActivity()


print (f"Data from {responseFileName}")
