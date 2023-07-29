# Reddit admin scripts

A repository of scripts used to perform some basic administrative tasks
on Reddit.

Created for use in r/Arduino, but should be generic enough to use on other
subreddits.

There are a couple of projects in this repository.


## Library/Common files.

These files are used by multiple scripts.

|File|Description|
|---|---|
|redditRequest.py|Submit a request to reddit via the API using an API token and record the JSON response for processing|
|redditGetToken.py|Request a fresh API token and cache it - only needed if the current token has expired|
|redditSecrets.py|Contains all of your secret stuff (passwords and user IDs). Also reads the API token from the cache.|

## Digest files

These files are used to extract posts and generate the monthly digest.

|File|Description|
|---|---|
|redditDigestExtract.py|Extract and record a summary of posts for a specified subreddit and month. This is needed if your subreddit has > 1,000 posts per month (which is the history limit)|
|redditDigestGenerate.py|Processes the extracted post summaries and generates the digest as Markdown. If provided, a header and footer will be included in the generated post.|


## Flair Extraction

This script is used to extract a list of users replying to milestone posts.

