/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.gm310509.reddit.admin;

import com.gm310509.reddit.comms.Token;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;



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
    
    
    private String getBearerAuthenticationHeader(String token) {
//        String valueToEncode = token;
//        return "Bearer " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
        return "Bearer " + token;
    }

    private Token token = new Token(tokenFileName);
    
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
                token.redditGetToken(retryCnt != 0);
                
                URL url = new URL(urlText);
    //            URL url = new URI(urlText).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);   // Connection timeout in ms
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", appName);
                conn.setRequestProperty("Authorization", getBearerAuthenticationHeader(token.getToken()));

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
