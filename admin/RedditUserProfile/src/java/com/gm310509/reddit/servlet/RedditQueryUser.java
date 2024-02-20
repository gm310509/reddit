/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.gm310509.reddit.servlet;

import com.gm310509.reddit.utility.SubActivityMetric;
import com.gm310509.reddit.utility.UserSummary;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author gm310509
 */
@WebServlet(name = "RedditQueryUser", urlPatterns = {"/RedditQueryUser"})
public class RedditQueryUser extends HttpServlet {

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
        response.setContentType("text/html;charset=UTF-8");
        
        String targetUserId = request.getParameter("targetUserId");
        
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

            out.println(
              String.format("<p>User activity for: <a href=\"https://www.reddit.com/user/%s\" target=\"redditTab\">u/%s</a></p>", targetUserId, targetUserId)
            );
            
            UserSummary summary = summariseUserActivity(targetUserId);
            if (summary != null) {
                int totalUserPostCount = 0;
            
                out.println("<table>");
                out.println("  <tr><th>Subreddit</th><th class=\"rightAlign\">Posts</th><th class=\"rightAlign\">Comments</th><th class=\"rightAlign\">Other</th><th class=\"rightAlign\">Total</th></tr>");

                for (String key : summary.keySet()) {
                    SubActivityMetric metric = summary.get(key);
                    int totalSubActivityCount = metric.getPostCount() + metric.getCommentCount() + metric.getOtherCount();
                    totalUserPostCount += totalSubActivityCount;
                    out.println(
                      String.format(
                              "  <tr><td><a href=\"https://www.reddit.com/r/%s\" target=\"redditTab\">%s</a></td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td><td class=\"rightAlign\">%d</td></tr>",
                              key,
                              key,
                              metric.getPostCount(),
                              metric.getCommentCount(),
                              metric.getOtherCount(),
                              totalSubActivityCount
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
    
    public UserSummary summariseUserActivity(String userName) {
        String fileName = String.format("/cygwin64/home/gm310509/user_reddit_%s.json", userName);
        UserSummary summary = new UserSummary(userName);
        String errorText = null;
        
        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
            StringBuilder jsonText = new StringBuilder();
            String inLine;
            while ((inLine = br.readLine()) != null) {
                jsonText.append(inLine);
            }
            
            Gson gson = new Gson();
            StringReader jsonStringReader = new StringReader(jsonText.toString());
            Map<?,?> map = gson.fromJson(jsonStringReader, Map.class);
            
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
            }

        } catch (FileNotFoundException e) {
            errorText = String.format("*** Error: file %s not found", fileName);
            System.out.println(errorText);
        } catch (IOException e) {
            errorText = String.format("*** Error reading: %s - %s", fileName, e.getMessage());
            System.out.println(errorText);
        }
        
        summary.setErrorText(errorText);
        return summary;
    }
}
