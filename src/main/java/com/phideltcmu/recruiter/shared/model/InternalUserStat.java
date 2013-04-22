package com.phideltcmu.recruiter.shared.model;

import java.io.Serializable;

public class InternalUserStat implements Serializable {
    public int getInternalID() {
        return internalID;
    }

    public void setInternalID(int internalID) {
        this.internalID = internalID;
    }

    public int getUniqueAdditions() {
        return uniqueAdditions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUniqueAdditions(int uniqueAdditions) {
        this.uniqueAdditions = uniqueAdditions;
    }

    private int internalID;
    private int uniqueAdditions;
    private String name;
}