package com.gdgcorehq.app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scheduled chapter meeting and published agenda.
 */
public class Meeting implements Serializable {

    private String id;
    private String title;
    private String date;
    private String time;
    private String location;
    private List<String> topics;
    private String publishedBy;
    private boolean isUpcoming;

    public Meeting() {
        this.topics = new ArrayList<>();
    }

    public Meeting(String id, String title, String date, String time, String location,
                   List<String> topics, String publishedBy, boolean isUpcoming) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.location = location;
        this.topics = topics != null ? topics : new ArrayList<>();
        this.publishedBy = publishedBy;
        this.isUpcoming = isUpcoming;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

    public boolean isUpcoming() { return isUpcoming; }
    public void setUpcoming(boolean upcoming) { isUpcoming = upcoming; }
}
