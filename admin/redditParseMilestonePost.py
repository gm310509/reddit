#!/usr/bin/python

import json
import re

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
  outputUserActivity()


responseFileName = "response.json"

with open(responseFileName, "r") as f:
  jsonData = json.load(f)

print(type(jsonData))
print("list len {}".format(len(jsonData)))

processReplies(jsonData)


print (f"Data from {responseFileName}")
