#!/usr/bin/python


# Library script to load a reddit API token.
# It will read the token from a cached copy.
# To get a fresh token, run the redditGetToken.py script.
# At the time of writing, reddit tokens lasted for about 24 hours.

import re

# UI for administering the Apps
redditMaintainAppsUri = "https://www.reddit.com/prefs/apps"     

# My personal reddit login details
redditUserId = "your reddit user ID"
redditUserPwd = "your reddit password here"


# My app details provided when registering an application
redditAppName = "your reddit App Name here"
redditRedirectUri = "A URL that can be used to redirect failed(?) requests - perhaps your home page" # not sure how this is used.

# My app details provided by reddit after App registration
redditClientId = "Your App client ID - a random string of characters allocated by reddit"           # ~22 chars
redditSecret   = "Your reddit App's secret text - a random string of characters allocated by reddit"   # ~30 chars


# variable to store the token.
tokenFileName = "/tmp/reddit-my.token"

# User agent for reddit requests:
userAgent = "{}/0.0.1".format(redditAppName)

# Initialise the OATH2 token to an empty string.
TOKEN=""

# Attempt to read the token file and store the token into a variable named TOKEN
# Clients can simply use the value of the TOKEN variable when submitting requests
# to the reddit API.
try:
  with open(tokenFileName, 'r') as f:
    tokenData = f.read()
    #print(tokenData)
    m = re.search("TOKEN=(.+)", tokenData)
    if (m is not None):
      TOKEN = m.group(1)
      #print(f"Token set to {TOKEN}")
except FileNotFoundError:
  print("No token file found")
