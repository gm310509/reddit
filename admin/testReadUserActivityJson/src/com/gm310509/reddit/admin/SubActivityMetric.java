/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.gm310509.reddit.admin;

/**
 *
 * @author gm310509
 */
public class SubActivityMetric {

    public SubActivityMetric(String subredditName) {
        this.subredditName = subredditName;
    }

    public SubActivityMetric() {
    }
    
    private String subredditName;

    /**
     * Get the value of subredditName
     *
     * @return the value of subredditName
     */
    public String getSubredditName() {
        return subredditName;
    }

    /**
     * Set the value of subredditName
     *
     * @param subredditName new value of subredditName
     */
    public void setSubredditName(String subredditName) {
        this.subredditName = subredditName;
    }

    private int postCount = 0;

    /**
     * Get the value of postCount
     *
     * @return the value of postCount
     */
    public int getPostCount() {
        return postCount;
    }

    /**
     * Set the value of postCount
     *
     * @param postCount new value of postCount
     */
    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    private int commentCount = 0;

    /**
     * Get the value of commentCount
     *
     * @return the value of commentCount
     */
    public int getCommentCount() {
        return commentCount;
    }

    /**
     * Set the value of commentCount
     *
     * @param commentCount new value of commentCount
     */
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    private int otherCount = 0;

    /**
     * Get the value of otherCount
     *
     * @return the value of otherCount
     */
    public int getOtherCount() {
        return otherCount;
    }

    /**
     * Set the value of otherCount
     *
     * @param otherCount new value of otherCount
     */
    public void setOtherCount(int otherCount) {
        this.otherCount = otherCount;
    }

    private int karma;

    /**
     * Get the value of karma
     *
     * @return the value of karma
     */
    public int getKarma() {
        return karma;
    }

    /**
     * Set the value of karma
     *
     * @param karma new value of karma
     */
    public void setKarma(int karma) {
        this.karma = karma;
    }
    
    private int maxUpVote = -999999;
    
    public int getMaxUpVote() {
        return this.maxUpVote;
    }

    private int maxDownVote = 0;
    
    public int getMaxDownVote() {
        return this.maxDownVote;
    }

    private int negativeScoreCnt = 0;
    
    public int getNegativeScoreCnt() {
        return this.negativeScoreCnt;
    }

    public int recordKarma(int votes) {
        this.karma += votes;
        if (votes > this.maxUpVote) {
            this.maxUpVote = votes;
        }
        if (votes < this.maxDownVote) {
            this.maxDownVote = votes;
        }
        
        if (votes < 0) {
            this.negativeScoreCnt++;
        }
        return this.karma;
    }

    public int incrementPostCount() {
        return ++postCount;
    }

    public int incrementCommentCount() {
        return ++commentCount;
    }

    public int incrementOtherCount() {
        return ++otherCount;
    }
}
