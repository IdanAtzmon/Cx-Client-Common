package com.cx.restclient.osa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by zoharby on 09/01/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CVE implements Serializable {

    private String id;
    private String cveName;
    private double score;
    private Severity severity;
    private String publishDate;//:"2016-11-07T10:16:06.1206743Z",
    private String url;//:"http://cv1",
    private String description;//:null,
    private String recommendations;//:"recommendation 1",
    private String sourceFileName;//:"SourceFileName 1",
    private String libraryId;//:"36b32b00-9ee6-4e2f-85c9-3f03f26519a9"
    private State state;
    private Integer commentsAmount;

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getCveName() {
        return cveName;
    }

    public void setCveName(String cveName) {
        this.cveName = cveName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getCommentsAmount() {
        return commentsAmount;
    }

    public void setCommentsAmount(Integer commentsAmount) {
        this.commentsAmount = commentsAmount;
    }
}
