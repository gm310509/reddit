#!/usr/bin/python

import redditSecrets

import requests
import json


def redditRequest(requestText, requestParameters,  responseFilename = None, sendToken = True, verbose = False):

  userAgent = "generic/0.0.1"
  if (sendToken):
    userAgent = "{}/0.0.1".format(redditSecrets.redditAppName)
  # setup our header info, which gives reddit a brief description of our app
  headers = {'User-Agent': redditSecrets.userAgent}

  if verbose:
    print("reddit App Name:  {}".format(redditSecrets.redditAppName))
    print("reddit client Id: {}".format(redditSecrets.redditClientId))
    print("reddit user Id:   {}".format(redditSecrets.redditUserId))
    print("user agent:       {}".format(userAgent))

    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
    print(f"****      OAuth TOKEN = {redditSecrets.TOKEN}")
    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")


  if (sendToken):
    # add authorization to our headers dictionary
    headers = {**headers, **{'Authorization': f"bearer {redditSecrets.TOKEN}"}}

  res = requests.get(requestText, headers=headers, params=requestParameters)
  jsonStr = ""
  if res.status_code != 200:
    print(f"ERROR: Response code: {res.status_code}: {res}")
    return
  if verbose:
    print("response:\n\n")
    print (res)
  headers = res.headers
  jsonStr = json.dumps(res.json())
  if "X-Ratelimit-Used" in headers:
    rateLimitUsed = headers["X-Ratelimit-Used"]
    rateLimitRemaining = headers["X-Ratelimit-Remaining"]
    rateLimitReset = headers["X-Ratelimit-Reset"]
    print (f"reddit request rate limits: Used: {rateLimitUsed}, Remaining: {rateLimitRemaining}, Reset: {rateLimitReset}")
  else:
    print("No rate limiting information in response")
  


  if verbose:
    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
    print(jsonStr)
    print("\nfrom {}".format(requestText))

  if responseFilename != None:
    with open(responseFilename, "w") as f:
      f.write(jsonStr)
      f.write("\n")
    if verbose:
      print(f"response written to {responseFilename}")

  return jsonStr


if __name__ == "__main__":
  requestParameters = {}

  # while the token is valid (~2 hours) we just add headers=headers to our requests
  #requestText = "https://oauth.reddit.com/api/v1/me"
  #requestText = "https://oauth.reddit.com/subreddits/mine/subscriber"
  #requestParameters = {"show" :"all", "sr_detail" : "all"}
  #requestText = "https://oauth.reddit.com/api/v1/me/karma"

  # Following is the post that is used to retrive a list of new posts.
  #requestText = "https://oauth.reddit.com/r/arduino/new"
  #requestParameters = {"limit" : 100 }
  # to get the next page:
  #requestParameters ["after"] = "t3_xc7jse"
  #requestParameters ["after"] = "t3_x9cesn"
  #requestParameters ["after"] = "t3_x5yya6"
  #requestParameters ["after"] = "t3_x3epzk"

  # Following is the post that is used to generate the list of users who participated in the 400K flare promotion.
  #requestText = "https://oauth.reddit.com/r/arduino/comments/w92qv3/congratulations_fellow_arduinauts_as_of_july_27th/?utm_source=share&utm_medium=web2x&context=3"

  # Following is the post that is used to generate the list of users who participated in the 500K flare promotion.
  requestText = "https://oauth.reddit.com/r/arduino/comments/zlwtq4/half_a_million_subscribers_enroll_here_to_receive/"
  requestText = "https://oauth.reddit.com/r/arduino/comments/11uqm3f/chatgpt_is_a_menace/"
  requestText = "https://www.reddit.com/r/arduino/comments/12dxdam/diy_arduino_video_game_console_arduino_pro_micro/"
  requestText = "https://oauth.reddit.com/r/arduino/comments/15cdeya/new_to_all_this_struggling_to_set_it_up"
  
  #requestText = "https://oauth.reddit.com/r/arduino/about/contributors"
  #requestText = "https://oauth.reddit.com/r/arduino/about/moderators"#
  #requestText = "https://oauth.reddit.com/r/arduino/about"
  #requestText = "https://www.reddit.com/r/arduino/about.json"           # Generates an error 403 (forbidden) - probably because of the token.
            # however, curl -A "testClient/1.0" https://www.reddit.com/r/arduino/about.json        # works just fine in cygwin.

  #requestText = "https://oauth.reddit.com/r/arduino"
  #requestText = "https://oauth.reddit.com/subreddits/mine/subscriber"
  #requestText = "https://oauth.reddit.com/user/gm310509/about"
  #requestText = "https://oauth.reddit.com/user/gm310509/overview"
  
  print("Sending: {}".format(requestText))
  responseFileName = "response.json"
  jsonStr = redditRequest(requestText, requestParameters, responseFileName, verbose = True)
  print(jsonStr)
  print(f"response written to {responseFileName}")
