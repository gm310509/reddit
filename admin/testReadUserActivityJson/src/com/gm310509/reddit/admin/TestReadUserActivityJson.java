/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.gm310509.reddit.admin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;



/**
 *
 * @author gm310509
 */
public class TestReadUserActivityJson {

    private static final String tokenFileName = "token.txt";
    
    private static Gson gson = new Gson();
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TestReadUserActivityJson main = new TestReadUserActivityJson();
        main.go(args);
    }
    
    public void go(String [] args) {
        System.out.println("Test read of user activity JSON from reddit.");
        
        if (args.length == 0) {
            System.out.println("Please provide at least one file on the command line");
            System.out.println("The file must be a JSON response to the following request");
            System.out.println("   https://www.reddit.com/user/<username>.json?limit=100");
            System.out.println("where <username> is a reddit username without the u/");
            usage();
            System.exit(0);
        }
        
        boolean online = false;
        boolean invalid = false;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (arg.equalsIgnoreCase("-o")) {
                    online = true;
                    break;
                } else if (arg.equalsIgnoreCase("-h")) {
                    usage();
                    System.exit(0);
                } else {
                    System.out.println(String.format("Invalid Argument '%s'", arg));
                    invalid = true;
                }
            }
        }
        if (invalid) {
            System.out.println("Invalid parameter.");
            usage();
            System.exit(1);
        }
        
        System.out.println(online ? "Online" : "Offline");

        for (String userName : args) {
            if (userName.startsWith("-")) {
                continue;
            }
            if (online) {
                processUserActivityFromUrl(userName);
            } else {
                processUserActivityFromFile(userName);
            }
        }
    }
    
    private void usage() {
        System.out.println("Usage:");
        System.out.println("  java com.gm310509.reddit.adminTestReadUserActivity [-o] userName [userName ...]");
        System.out.println("  java com.gm310509.reddit.adminTestReadUserActivity -h");
        System.out.println("Where:");
        System.out.println("  -o enables online queries.");
        System.out.println("  -h prints this help and exits.");
    }
    
    private Map<?,?> getMap(Map<?,?> map, String key) {
        if (map.containsKey(key)) {
            Object o = map.get(key);
            if (o instanceof Map map1) {
                return map1;
            }
        }
        return null;
    }
    
    private ArrayList<?> getArrayList(Map<?,?> map, String key) {
        if (map.containsKey(key)) {
            Object o = map.get(key);
            if (o instanceof ArrayList arrayList) {
                return arrayList;
            }
        }
        return null;
    }
    

    private String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
    private String getBearerAuthenticationHeader(String token) {
//        String valueToEncode = token;
//        return "Bearer " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
        return "Bearer " + token;
    }

    /* Potential repsonses from reddit:
    *  Wrong credentials or expired token:
    *   {"message": "Unauthorized", "error": 401}
    *
    *  User not authorised to access the registered "reddit App":
    *   {'error': 'invalid_grant'}
    *
    *  Token:
    *   {'access_token': 'eyJhbGciOiJSUzI1NiIsImtpZCI6IlNIQTI1NjpzS3dsMnlsV0VtMjVmcXhwTU40cWY4MXE2OWFFdWFyMnpLMUdhVGxjdWNZIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ1c2VyIiwiZXhwIjoxNzA2ODYwMjA3LjY4OTM3OSwiaWF0IjoxNzA2NzczODA3LjY4OTM3OSwianRpIjoiR3JJeUNtdDQ0VFJmTjRHQjhUd3Ayd2QzR0k3emVBIiwiY2lkIjoiSkZCbkZuYU9hOU1yb1JlTGtKZTBGUSIsImxpZCI6InQyX3I4aDh2YnF4dCIsImFpZCI6InQyX3I4aDh2YnF4dCIsImxjYSI6MTcwNDIwMDQ2OTA0Mywic2NwIjoiZUp5S1Z0SlNpZ1VFQUFEX193TnpBU2MiLCJmbG8iOjl9.q5Wf5pc6ST0dwYGF14AUiVEQ_o7vJWePARlROM3OJCI3GcaBkUUibKz3awu32a1nuUp6E0Zpg69GQ-gZKpYUiRoqxnNQdsdLRXDVRyyc5qnrhMAWhpMphW6R62uPI_JQvv1CtrSAAz6WaE2Z6eBoTHhzSwdbH6lm3O9NItHMY588O4bK46rV2WfTC_D-dwRIKLzblC51Snn8IDgOWj154rY8XNApDrKtLOtazRPmWZWJWhAxZ2cvqfHWKgSfjS54ev3Q8Niwz8QQstByW8Sm9D_PPF9406hxyLT-fmxUA9CEXsZHB1lGPznReCDqwCGRuvtvntMJ09YRCBl4sBLOZg', 'token_type': 'bearer', 'expires_in': 86400, 'scope': '*'}
    */
    private String _token = null;
    
    public String redditGetToken() {
        return redditGetToken(false);
    }
    
    public String redditGetToken(boolean force) {

        Path tokenFilePath = Paths.get(tokenFileName);
        if (!force) {           // If we are not forcing a token retrieval, then 
            if (_token != null) {    // check to see if we already have a token
                System.out.println("Reusing existing token: " + _token);
                return _token;       // we do have a token, so return it.
            }
        
            if (Files.exists(tokenFilePath)) {
                System.out.println(String.format("Attempting to read existing token from %s", tokenFileName));
                try {
                    BufferedReader br = new BufferedReader(new FileReader(tokenFilePath.toFile()));
                    StringBuilder fileText = new StringBuilder();
                    String inLine;
                    while ((inLine = br.readLine()) != null) {
                        fileText.append(inLine);
                    }
                    _token = fileText.toString();
                    return _token;
                } catch (FileNotFoundException e) {
                    System.out.println(String.format("token file %s not found - requesting new token", tokenFileName));
                } catch (IOException e) {
                    System.out.println(String.format("IO Exception reading %s - requesting new token", tokenFileName));
                }
            }
        } else {
            System.out.println("Force is true, requesting a new token from the OAUTH2 server");
        }
        
        // Either we are forcing the request of a new token, or, we don't
        // already have one.
        System.out.println("requesting oauth2 token");
        
//        HttpClient client = HttpClient.newHttpClient();
        HttpClient client = HttpClient.newHttpClient();
        
        String oAuth2UrlText = "https://www.reddit.com/api/v1/access_token";
        
        String userId = "gm310509Service";
        String password = "m!bElu<_4m/>";
        String redditAppName = "gmcMod";
        //My app details provided by reddit after registration
        String redditClientId = "JFBnFnaOa9MroReLkJe0FQ";           // ~22 chars
        String redditClientPwd = "ai6ZJiSnWwxQXVRh-hp11ejVZi75Ig";   // ~30 chars
        String appName = String.format("%s/0.0.1", redditAppName);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(oAuth2UrlText))
            .header("Authorization", getBasicAuthenticationHeader(redditClientId, redditClientPwd))
            .header("User-Agent", appName)
            .POST(HttpRequest.BodyPublishers.ofString(String.format("grant_type=password&username=%s&password=%s", userId, password)))
            .build();
        
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            System.out.println(String.format("Status: %d", status));

            gson = new Gson();
            StringReader jsonStringReader = new StringReader(response.body());
            Map<?,?> map = gson.fromJson(jsonStringReader, Map.class);
            
            if (map.containsKey("message")) {
                String message = (String) map.get("message");
                Double errorCode = (Double) map.get("error");
                System.out.println("Error reply from reddit:");
                System.out.println("message: " + message);
                System.out.println("error: " + errorCode);
            } else if (map.containsKey("error")) {
                String errorMessage = (String) map.get("error");
                System.out.println("error: " + errorMessage);
            } else if (map.containsKey("access_token")) {
                System.out.println("Got a token!");
                _token = (String) map.get("access_token");
                String tokenType = (String) map.get("token_type");
                Double expires = (Double) map.get("expires_in");
                String scope = (String) map.get("scope");
                System.out.println("token: " + _token);
                System.out.println("type: " + tokenType + ", scope: " + scope + ", expires: " + expires);
            } else {
                System.out.println("*** Unrecognised oauth2 response");
                System.out.println("Response to oAuth2 request: ");
                System.out.println(response);
                System.out.println("------\nBody:");
                System.out.println(response.body());
                System.out.println(String.format("type: %s", response.getClass().getCanonicalName()));
            }

        } catch (IOException e) {
            System.out.println("IOException attempting to retrieve token.");
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception attempting to retrieve token.");
        }
        
        // Write the token out to our token file cache.
        try {
            Files.deleteIfExists(tokenFilePath);
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(tokenFilePath.toFile()));
            bw.write(_token);
            bw.close();
        } catch (IOException e) {
            System.out.println(String.format("IO Error writing token to token file: %s", tokenFileName));
            System.out.println(e.toString());
        }
        return _token;
    }
    
    public void processUserActivityFromUrl(String userName) {
        String urlText = String.format("https://oauth.reddit.com/user/%s.json?limit=100",userName);
        System.out.println(urlText);
        UserSummary summary = new UserSummary(userName);
        

        String redditAppName = "gmcMod";
        String appName = String.format("%s/0.0.1", redditAppName);

        int retryCnt = 0;
        
        try {
            do {
                System.out.println(String.format("Reading data for: %s (attempt: %d)", userName, retryCnt));
                // TODO: If the request returns a 401 error (not authorised), try
                // requesting a new token, then resubmit the query.
                // If it fails after the second attempt, give up.
                // If we are retrying to get a token, force a fetch from the OAUTH2 server.
                String token = redditGetToken(retryCnt != 0);
                
                URL url = new URL(urlText);
    //            URL url = new URI(urlText).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);   // Connection timeout in ms
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", appName);
                conn.setRequestProperty("Authorization", getBearerAuthenticationHeader(token));

    //            System.out.println(String.format("%d Header fields", conn.getHeaderFields().size()));
    //            for (String key : conn.getHeaderFields().keySet()) {
    //                System.out.println(String.format("key=%s,value=%s", key, conn.getRequestProperty(key)));
    //            }

                BufferedReader reader;
                int status = conn.getResponseCode();
                if (status >= 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                }

                summary = summariseUserActivity(reader, userName);
                retryCnt++;
            } while (summary.getErrorNum() != 0
                    && retryCnt < 2
                    && summary.getErrorNum() == 401 /* Invalid token/Unauthorised */
                );

        } catch (IOException e) {
            summary = new UserSummary(userName);
            summary.setErrorText(String.format("IOException reading %s", urlText));
        } finally {
            reportUserActivity(summary);
        }
    }
    
    
    public void processUserActivityFromFile(String userName) {
        String fileName = String.format("user_reddit_%s.json", userName);
        System.out.println(String.format("Reading from: %s", fileName));
        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
            UserSummary summary = summariseUserActivity(br, userName);
            reportUserActivity(summary);
        } catch (FileNotFoundException e) {
            System.out.println(String.format("*** Error: file %s not found", fileName));
        } catch (IOException e) {
            System.out.println(String.format("*** Error reading: %s - %s", fileName, e.getMessage()));
        }
    }
    
    
    public UserSummary summariseUserActivity(BufferedReader reader, String userName)
        throws IOException {

        UserSummary summary = new UserSummary(userName);
        
        StringBuilder jsonText = new StringBuilder();
        String inLine;
        while ((inLine = reader.readLine()) != null) {
            jsonText.append(inLine);
        }
        
        gson = new Gson();
        try {
            StringReader jsonStringReader = new StringReader(jsonText.toString());
            Map<?,?> map = gson.fromJson(jsonStringReader, Map.class);
//            System.out.println(map);    // BEWARE: Can generate a huge output.

            Map<?,?> data = getMap(map, "data");
            if (data != null) {
                ArrayList<?> posts = getArrayList(data,"children");
                if (posts != null) {
                    for (Object pm : posts) {
                        if (pm instanceof Map postMeta) {
                            String postKind = "unknown";
                            String postSubRedditName = "unknown";
                            if (postMeta.containsKey("kind")) {
                                postKind = (String) postMeta.get("kind");
                            }
                            if (postMeta.containsKey("data")) {
                                Object pd = postMeta.get("data");
                                if (pd instanceof Map postData) {
                                    postSubRedditName = (String) postData.get("subreddit");
                                }
                            }
//                            System.out.println(String.format("Post %s to r/%s", postKind, postSubRedditName));
                            if ("t3".equalsIgnoreCase(postKind)) {
                                summary.countPost(postSubRedditName);
                            } else if ("t1".equalsIgnoreCase(postKind)) {
                                summary.countComment(postSubRedditName);
                            } else {
                                summary.countOther(postSubRedditName);
                            }
                        }
                    }
                }
            } else if (map.containsKey("error")) {
                int errorCode = ((Double) map.get("error")).intValue();
                if (map.containsKey("message")) {
                    summary.setError(errorCode, (String) map.get("message"));
                } else {
                    summary.setError(errorCode, String.format("Error %d retrieving user data for %s", errorCode, userName));
                }
            } else {
                summary.setError(-1, "Invalid response: " + jsonText.toString());
            }
        } catch (JsonSyntaxException e) {
            summary.setErrorText("JSON Syntax Exception: " + e.getMessage());
            System.out.println("JSON Syntax Exception: " + e.getMessage());
            System.out.println(jsonText.toString());
        }
        return summary;
    }
    
    
    public void reportUserActivity (UserSummary summary) {
        if (summary.getErrorText() != null) {
            System.out.println();
            System.out.println(String.format("*** Error: %d, \"%s\" for user %s.", summary.getErrorNum(), summary.getErrorText(), summary.getName()));
            System.out.println();
        }
        
        System.out.println(String.format("\nUser activity for %s:", summary.getName()));
        System.out.println(String.format("sub reddit name                posts cmnts other total"));
        int totalPosts = 0;
        for (SubActivityMetric metric : summary.values()) {
            int totalActivityInSub = metric.getPostCount() + metric.getCommentCount() + metric.getOtherCount();
            System.out.println(
                String.format("%-30s %5d %5d %5d %5d",
                    metric.getSubredditName(),
                    metric.getPostCount(),
                    metric.getCommentCount(),
                    metric.getOtherCount(),
                    totalActivityInSub
                )
            );
            totalPosts += totalActivityInSub;
        }
        System.out.println(String.format("Total: %d subs, %d posts", summary.size(), totalPosts));
        System.out.println();
    }
}
