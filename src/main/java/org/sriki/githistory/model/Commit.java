package org.sriki.githistory.model;

import java.time.ZonedDateTime;


public class Commit implements Comparable<Commit> {
    private int id;
    private String commitId;
    private String author;
    private String reviewer;
    private String parent;
    private String message;
    private ZonedDateTime authTime;
    private ZonedDateTime mergeTime;
    private String authEmail;
    private String mergeEmail;
    private String tag;
    private int tickerNum;
    private String project;
    private int fileCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ZonedDateTime getAuthTime() {
        return authTime;
    }

    public void setAuthTime(ZonedDateTime authTime) {
        this.authTime = authTime;
    }

    public ZonedDateTime getMergeTime() {
        return mergeTime;
    }

    public void setMergeTime(ZonedDateTime mergeTime) {
        this.mergeTime = mergeTime;
    }

    public String getAuthEmail() {
        return authEmail;
    }

    public void setAuthEmail(String authEmail) {
        this.authEmail = authEmail;
    }

    public String getMergeEmail() {
        return mergeEmail;
    }

    public void setMergeEmail(String mergeEmail) {
        this.mergeEmail = mergeEmail;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getTickerNum() {
        return tickerNum;
    }

    public void setTickerNum(int tickerNum) {
        this.tickerNum = tickerNum;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    @Override
    public String toString() {
        return "Commit{" +
                "id=" + id +
                ", commitId='" + commitId + '\'' +
                ", author='" + author + '\'' +
                ", reviewer='" + reviewer + '\'' +
                ", parent='" + parent + '\'' +
                ", message='" + message + '\'' +
                ", authTime=" + authTime +
                ", mergeTime=" + mergeTime +
                ", authEmail='" + authEmail + '\'' +
                ", mergeEmail='" + mergeEmail + '\'' +
                ", tag='" + tag + '\'' +
                ", tickerNum=" + tickerNum +
                ", project='" + project + '\'' +
                '}';
    }

    @Override
    public int compareTo(Commit other) {
        return this.mergeTime.compareTo(other.mergeTime);
    }
}
