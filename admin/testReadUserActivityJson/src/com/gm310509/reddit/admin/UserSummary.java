/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.gm310509.reddit.admin;

import java.util.HashMap;

/**
 *
 * @author gm310509
 */
public class UserSummary extends HashMap<String, SubActivityMetric> {
    

//    public UserSummary() {
//    }


    public UserSummary(String name) {
        this.name = name;
    }



    private String name;

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public SubActivityMetric countPost(String subRedditName) {
        SubActivityMetric metric = retrieve(subRedditName);
        metric.incrementPostCount();
        return metric;
    }
    
    public SubActivityMetric countComment(String subRedditName) {
        SubActivityMetric metric = retrieve(subRedditName);
        metric.incrementCommentCount();
        return metric;
    }
    
    public SubActivityMetric countOther(String subRedditName) {
        SubActivityMetric metric = retrieve(subRedditName);
        metric.incrementOtherCount();
        return metric;
    }


    /**
     * Retrieve a metric for the named subreddit.
     * Create the metric if it doesn't already exist.
     * 
     * @param subredditName
     * @return a SubActityMetric for the named subreddit
     */
    SubActivityMetric retrieve(String subredditName) {
        if (containsKey(subredditName))
            return (get(subredditName));
        else {
            SubActivityMetric metric = new SubActivityMetric(subredditName);
            put(subredditName, metric);
            return metric;
        }
    }
    
    private int errorNum;

    /**
     * Get the value of errorNum
     *
     * @return the value of errorNum
     */
    public int getErrorNum() {
        return errorNum;
    }

    /**
     * Set the value of errorNum
     *
     * @param errorNum new value of errorNum
     */
    public void setErrorNum(int errorNum) {
        this.errorNum = errorNum;
    }

    
    private String errorText = null;

    /**
     * Get the value of errorText
     *
     * @return the value of errorText
     */
    public String getErrorText() {
        return errorText;
    }

    /**
     * Set the value of errorText
     *
     * @param errorText new value of errorText
     */
    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
    
    
    /**
     * Sets both the error number and error text.
    */
    public void setError(int errorNum, String errorText) {
        this.errorNum = errorNum;
        this.errorText = errorText;
    }

}
