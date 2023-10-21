#!/usr/bin/python3

import redditSecrets
import requests

def getToken(verbose = False):
  if verbose:
    print("reddit App Name:  {}".format(redditSecrets.redditAppName))
    print("reddit client Id: {}".format(redditSecrets.redditClientId))
    print("reddit user Id:   {}".format(redditSecrets.redditUserId))

  userAgent = "{}/0.0.1".format(redditSecrets.redditAppName)
  if verbose:
    print("user agent:       {}".format(userAgent))

  # exit(1)

  # note that CLIENT_ID refers to 'personal use script' and SECRET_TOKEN to 'token'
  auth = requests.auth.HTTPBasicAuth(redditSecrets.redditClientId, redditSecrets.redditSecret)

  # here we pass our login method (password), username, and password
  data = {'grant_type': 'password',
          'username': redditSecrets.redditUserId,
          'password': redditSecrets.redditUserPwd }

  # setup our header info, which gives reddit a brief description of our app
  headers = {'User-Agent': redditSecrets.userAgent}

  # send our request for an OAuth token
  res = requests.post('https://www.reddit.com/api/v1/access_token',
                      auth=auth, data=data, headers=headers)

  if verbose:
    print("OAuth response:\n\n")
    print(res)
    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
    print(res.json())

  # convert response to JSON and pull access_token value
  TOKEN = res.json()['access_token']

  if verbose:
    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
    print(f"The following will be saved to {redditSecrets.tokenFileName}")
    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
    print(f"TOKEN={TOKEN}")

  with open(redditSecrets.tokenFileName, 'w') as f:
    f.write(f"TOKEN={TOKEN}")

  if verbose:
    print("Done.")

if __name__ == "__main__":
  getToken(True)

