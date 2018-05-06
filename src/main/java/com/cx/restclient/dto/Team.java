package com.cx.restclient.dto;

/**
 * Created by Galn on 14/02/2018.
 */
public class Team {
    private String id;
    private String fullName;

    public Team() {
    }

    public Team(String id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
