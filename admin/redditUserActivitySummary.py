#!/usr/bin/python

import json
import requests
import sys

#data from:
# curl -A "testClient/1.0"  https://www.reddit.com/user/radarOverhead.json?limit=100 >radarOverhead_user.json


userActivity = {}
subredditCnt=0
postCnt=0
userName=""
retrievalLimit=100

def processUserActivity(jsonData):
  global postCnt, subredditCnt, userActivity, userName

  print("Generating Posts/comments summary:")
  for elem in jsonData:
    #print (type(elem))
    #print("elem len {}".format(len(elem)))
    #print(f"Processing element {postCnt}")
    postCnt = postCnt + 1
    subredditName = "ZZZunknown"
    if ("data" in elem):
      post = elem["data"]
      if ("subreddit" in post):
        subredditName = post["subreddit"]

    if subredditName in userActivity:
      userActivity[subredditName] += 1
    else:
      userActivity[subredditName] = 1
    #print(f"Subreddit name: {subredditName}")

  print(f"Summary of posts for user: {userName}")    
  for subredditName in sorted(userActivity):
    print ("{:5}  {}".format(userActivity[subredditName], subredditName))
    subredditCnt += 1
  print ("Total: {} posts (max:{}) in {} subreddits".format(postCnt, retrievalLimit, subredditCnt))


if len(sys.argv) != 2:
  print("Please specify the user name on the command line")
  sys.exit(1)

#userName = "radarOverhead"
userName = sys.argv[1]

responseFileName = f"user_reddit_{userName}.json"

requestText = f"https://www.reddit.com/user/{userName}.json?limit={retrievalLimit}"

print(requestText)
print(responseFileName)

headers = {'User-Agent': "testClient/1.0"}
response = requests.get(requestText, headers=headers)

print(f"Response: {response.status_code} - {requests.status_codes._codes[response.status_code][0]}")
if (response.status_code != 200):
  print (response)
  sys.exit(1)

jsonData = response.json()
jsonStr = json.dumps(jsonData)

with open(responseFileName, "w") as f:
  f.write(jsonStr)

#with open(responseFileName, "r") as f:
#  jsonData = json.load(f)


#print(type(jsonData))
#print("list len {}".format(len(jsonData)))

if ("data" in jsonData):
  userData = jsonData["data"]
  if ("children" in userData):
    processUserActivity(userData["children"])
  else:
    print("poorly formed reply, no 'children' element in json");
    print (jsonData)
else:
  print("poorly formed reply, no 'data' element in json");
  print (jsonData)

print (f"Data saved in {responseFileName}")
