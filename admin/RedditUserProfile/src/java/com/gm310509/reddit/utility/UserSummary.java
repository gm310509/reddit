/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.gm310509.reddit.utility;

import java.util.HashMap;

/**
 *
 * @author gm310509
 */
public class UserSummary extends HashMap<String, SubActivityMetric> {
    

    public UserSummary() {
    }


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
    
    public SubActivityMetric countPost(String subRedditName, int votes) {
        SubActivityMetric metric = retrieve(subRedditName);
        metric.incrementPostCount();
        metric.recordKarma(votes);
        return metric;
    }
    
    public SubActivityMetric countComment(String subRedditName, int votes) {
        SubActivityMetric metric = retrieve(subRedditName);
        metric.incrementCommentCount();
        metric.recordKarma(votes);
        return metric;
    }
    
    public SubActivityMetric countOther(String subRedditName, int votes) {
        SubActivityMetric metric = retrieve(subRedditName);
        metric.incrementOtherCount();
        metric.recordKarma(votes);
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

}
