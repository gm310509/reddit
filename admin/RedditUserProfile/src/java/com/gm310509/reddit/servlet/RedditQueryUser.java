/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.gm310509.reddit.servlet;

import com.gm310509.reddit.utility.SubActivityMetric;
import com.gm310509.reddit.utility.Token;
import com.gm310509.reddit.utility.UserSummary;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Version history:
 * 1.00.00.00 - Initial Version
 *
 * 1.01.00.00 - Added form to results display.
 *              Added capture of karma metrics for each subreddit.
 */

/**
 * 
 * @author gm310509
 */
@WebServlet(name = "RedditQueryUser", urlPatterns = {"/RedditQueryUser"})
public class RedditQueryUser extends HttpServlet {

    public final int MIN_RECORDS = 10;
    public final int MAX_RECORDS = 1000;
    
    public static final String VERSION = "1.01.00.00";
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String errorMessage = null;
        int limit = 100;
        String targetUserId = request.getParameter("targetUserId");
        String limitText = request.getParameter("limit");
        try {
            limit = Integer.parseInt(limitText);
        } catch (NumberFormatException e) {
            errorMessage = String.format("Invalid limit '%s' (not an integer)", limitText);
        }
        
        if (limit < MIN_RECORDS || limit > MAX_RECORDS) {
            int badLimit = limit;
            limit = Math.max(MIN_RECORDS, Math.min(limit, MAX_RECORDS));
            errorMessage = String.format("Limit (%d) out of range [%d ... %d] using %d.", badLimit,MIN_RECORDS, MAX_RECORDS, limit);
        }

        System.out.printf("RedditQueryUser: From: %s. user: %s, limit: %s\n", request.getRemoteAddr(), targetUserId, limitText);
                
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Reddit User Activity Profile</title>");
            out.println("<link rel=\"stylesheet\" href=\"css/stylesheet.css\">");
            out.println("</head>");
            out.println("<body>");
//            out.println("<h1>Servlet RedditQueryUser at " + request.getContextPath() + "</h1>");
            out.println("<h1>Reddit User Activity Profile: u/" + targetUserId + "</h1>");

            out.println(String.format("    <span class=\"annotation\">Version: %s</span>", VERSION));
            out.println(
              String.format("<p>User activity for: <a href=\"https://www.reddit.com/user/%s\" target=\"redditTab\">u/%s</a></p>", targetUserId, targetUserId)
            );
            
            if (errorMessage != null) {
                out.println("<p class=\"errorText\">" + errorMessage + "</p>");
            }
            
            UserSummary summary = summariseUserActivity(targetUserId, limit);
            if (summary != null) {
                int totalUserPostCount = 0;
            
                out.println("<table>");
                out.println("  <tr><th>&nbsp;</th><th colspan=\"4\">Posts</th><th colspan=\"4\">Karma</th></tr>");
                out.println("  <tr><th>Subreddit</th><th class=\"rightAlign\">Posts</th><th class=\"rightAlign\">Comments</th><th class=\"rightAlign\">Other</th><th class=\"rightAlign\">Total</th><th class=\"rightAlign\">Total</th><th class=\"rightAlign\">Max up score</th><th class=\"rightAlign\">Max down score</th><th class=\"rightAlign\"># -ve vote posts</th></tr>");

                for (String key : summary.keySet()) {
                    SubActivityMetric metric = summary.get(key);
                    int totalSubActivityCount = metric.getPostCount() + metric.getCommentCount() + metric.getOtherCount();
                    totalUserPostCount += totalSubActivityCount;
                    out.println(
                      String.format(
                            "  <tr><td><a href=\"https://www.reddit.com/r/%s\" target=\"redditTab\">%s</a></td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td></tr>",
                            key,
                            key,
                            metric.getPostCount(),
                            metric.getCommentCount(),
                            metric.getOtherCount(),
                            totalSubActivityCount,
                            metric.getKarma(),
                            metric.getMaxUpVote(),
                            metric.getMaxDownVote(),
                            metric.getNegativeScoreCnt()
                              
                      )
                    );
//                    System.out.println(
//                        String.format("%-30s %5d %5d %5d %5d",
//                            metric.getSubredditName(),
//                            metric.getPostCount(),
//                            metric.getCommentCount(),
//                            metric.getOtherCount(),
//                            totalSubActivityCount
//                        )
//                    );
                }

                out.println("</table>");

                out.println(String.format("<p>Total User Activity: %d posts.</p>", totalUserPostCount));
                
                if (summary.getErrorText() != null) {
                    out.println(String.format("<p class=\"errorText\">Error: %s</p>", summary.getErrorText()));
                }
                
                out.println("<form name=\"queryForm\" id=\"frmQuery\" action=\"RedditQueryUser\" method=\"post\">");
                out.println("    <p>");
                out.println("        <label for=\"target\">Reddit user name: u/</label>");
                out.println("        <input type=\"text\" name=\"targetUserId\" id=\"target\" size=\"30\" placeholder=\"user name\" required autofocus/>");
                out.println("    </p>");
                out.println("    <p>");
                out.println("    <label for=\"limit\">History to retrieve: </label>");
                out.println("    <input type=\"number\" name=\"limit\" id=\"limit\" size=\"10\" placeholder=\"records\" required autofocus value=\"100\"/>");
                out.println("    <br>");
                out.println("    <span class=\"annotation\">NB: Reddit seems to return a maximum of 100 records - even if you request more.</span>");
                out.println("    </p>");
                out.println("    <br><br>");
                out.println("    ");
                out.println("    <input type=\"submit\" name=\"submitButton\" id=\"submit\" value=\"Submit\"/>");
                out.println("</form>");
                
            } else {
                out.println("<p class=\"errorText\">Null returned from summariseUserActivity(String username);</p>");
            }
            
            out.println("<p><a href=\"index.jsp\">Back to Index</a></p>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    
    
    
    private Map<?,?> getMap(Map<?,?> map, String key) {
        if (map.containsKey(key)) {
            Object o = map.get(key);
            if (o instanceof Map) {
                Map map1 = (Map) o;
                return map1;
            }
        }
        return null;
    }
    
    private ArrayList<?> getArrayList(Map<?,?> map, String key) {
        if (map.containsKey(key)) {
            Object o = map.get(key);
            if (o instanceof ArrayList) {
                ArrayList arrayList = (ArrayList) o;
                return arrayList;
            }
        }
        return null;
    }
    
    
    
    
    
    public UserSummary summariseUserActivity(String userName, int limit) {
        UserSummary summary = new UserSummary(userName);
        String errorText = null;
        
        int retryCnt = 0;
        int errorCode = 0;
        do {
            try {
//                Reader reader = getUserActivityFromFile(userName);
                Reader reader = getUserActivityFromOnline(userName, limit, retryCnt != 0);
                Gson gson = new Gson();
                Map<?,?> map = gson.fromJson(reader, Map.class);

                if (map.containsKey("data")) {
                    Map<?,?> data = getMap(map, "data");
                    if (data != null) {
                        ArrayList<?> posts = getArrayList(data,"children");
                        if (posts != null) {
                            for (Object pm : posts) {
                                if (pm instanceof Map) {
                                    Map postMeta = (Map) pm;
                                    String postKind = "unknown";
                                    String postSubRedditName = "unknown";
                                    if (postMeta.containsKey("kind")) {
                                        postKind = (String) postMeta.get("kind");
                                    }
                                    
                                    int votes = 0;
                                    if (postMeta.containsKey("data")) {
                                        Object pd = postMeta.get("data");
                                        if (pd instanceof Map) {
                                            Map postData = (Map) pd;
                                            postSubRedditName = (String) postData.get("subreddit");
                                            if (postData.containsKey("ups")) {
                                                votes = ((Double) postData.get("ups")).intValue();
                                            }
                                        }
                                    }
        //                            System.out.println(String.format("Post %s to r/%s", postKind, postSubRedditName));
                                    if ("t3".equalsIgnoreCase(postKind)) {
                                        summary.countPost(postSubRedditName, votes);
                                    } else if ("t1".equalsIgnoreCase(postKind)) {
                                        summary.countComment(postSubRedditName, votes);
                                    } else {
                                        summary.countOther(postSubRedditName, votes);
                                    }
                                }
                            }
                        }
                    }
                } else if (map.containsKey("error")) {
                    errorCode = ((Double) map.get("error")).intValue();
                    String msg = "unknown error";
                    if (map.containsKey("message")) {
                        msg = (String) map.get("message");
                    }
                    System.out.println(String.format("Error in reddit response to request for user activity (about.json): %d - %s", errorCode, msg));
                } else {
                    errorCode = -1;
                    System.out.println("Result from reddit does not contain a data element. Top level keys in response:");
                    Iterator it = map.keySet().iterator();
                    while (it.hasNext()) {
                        System.out.println("  " + it.next());
                    }
                }

//            } catch (FileNotFoundException e) {
//                errorText = String.format("*** Error: file %s not found", fileName);
//                System.out.println(errorText);
//            } catch (IOException e) {
//                errorText = String.format("*** Error reading: %s - %s", fileName, e.getMessage());
//                System.out.println(errorText);
            } catch (MalformedURLException ex) {
                System.out.println("Malformed URL Exception: " + ex.toString());
                ex.printStackTrace();
            } catch (ProtocolException ex) {
                System.out.println("Protocol Exception: " + ex.toString());
                ex.printStackTrace();
            } catch (IOException e) {
                System.out.println("IO Exception reading from reddit: " + e.toString());
                e.printStackTrace();
            } catch (Throwable t) {
                System.out.println("Unexpected exception: " + t.getMessage());
                t.printStackTrace();
            } finally {
                retryCnt ++;
            }
        } while (errorCode != 0 && retryCnt < 2);
            
        
        summary.setErrorText(errorText);
        return summary;
    }
    
       
    private String getBearerAuthenticationHeader(String token) {
//        String valueToEncode = token;
//        return "Bearer " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
        return "Bearer " + token;
    }
 
    private static Token token = new Token();


    private Reader getUserActivityFromOnline(String userName, int limit, boolean force)
            throws MalformedURLException, ProtocolException, IOException {
        String urlText = String.format("https://oauth.reddit.com/user/%s.json?limit=%d",userName, limit);
        String redditAppName = "gmcMod";
        String appName = String.format("%s/0.0.1", redditAppName);

        System.out.println(String.format("Querying user activity: %s (limit: %d)\nURL: %s", userName, limit, urlText));
    
        token.redditGetToken(force);
        URL url = new URL (urlText);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);   // Connection timeout in ms
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", appName);
        conn.setRequestProperty("Authorization", getBearerAuthenticationHeader(token.getToken()));
        BufferedReader reader;
        int status = conn.getResponseCode();
        if (status >= 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }
        return reader;
    }
    
    
    private String fileName = "";
    private Reader getUserActivityFromFile(String userName)
    throws FileNotFoundException, IOException {
        fileName = String.format("/cygwin64/home/gm310509/user_reddit_%s.json", userName);
        FileReader fr = new FileReader(fileName);
        BufferedReader br = new BufferedReader(fr);
        StringBuilder jsonText = new StringBuilder();
        String inLine;
        while ((inLine = br.readLine()) != null) {
            jsonText.append(inLine);
        }
        StringReader jsonStringReader = new StringReader(jsonText.toString());
        return jsonStringReader;
    }
}
