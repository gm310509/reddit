#!/usr/bin/python3

import sys
import redditSecrets
import requests

def getToken(verbose = False, authenticatorToken = None):
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
  password = redditSecrets.redditUserPwd
  if (authenticatorToken is not None):
    password += ":" + authenticatorToken

  data = {'grant_type': 'password',
          'username': redditSecrets.redditUserId,
          'password': password }

  # setup our header info, which gives reddit a brief description of our app
  headers = {'User-Agent': redditSecrets.userAgent}

  # send our request for an OAuth token
  res = requests.post('https://www.reddit.com/api/v1/access_token',
                      auth=auth, data=data, headers=headers)

  response = res.json()
  if verbose:
    print("OAuth response:\n\n")
    print(res)
    print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
    print(response)

  # convert response to JSON and pull access_token value
  if ("access_token" in response):
    TOKEN = response['access_token']

    if verbose:
      print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
      print(f"The following will be saved to {redditSecrets.tokenFileName}")
      print("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n\n")
      print(f"TOKEN={TOKEN}")

    with open(redditSecrets.tokenFileName, 'w') as f:
      f.write(f"TOKEN={TOKEN}")
  elif ("error" in response):
    errorText = response["error"]
    print("\n\nError response from reddit")
    print(f"Error text: {errorText}")
    print("consider using 2FA (provide an authenticator code for reddit as the 1st parameter to this script)")
  else:
    print("Unrecognised response from reddit:")
    print(response)

  if verbose:
    print("Done.")

if __name__ == "__main__":
  authenticatorToken = None
  if (len(sys.argv) == 2):
    if (sys.argv[1] == "-h"):
      print("usage:")
      print(f"  {sys.argv[0]} [ -h | 2FA authenticator token ]")
      sys.exit(0)

    authenticatorToken = sys.argv[1]
  
  getToken(True, authenticatorToken)

