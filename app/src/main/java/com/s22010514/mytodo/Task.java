package com.s22010514.mytodo;

public class Task {

    private int id;
    private String title;
    private String description;
    private long dateInMillis;
    private boolean isCompleted;

    public Task(int id, String title, String description, long dateInMillis) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateInMillis = dateInMillis;
        this.isCompleted = false; // Default to not completed
    }

    public Task(int id, String title, String description, long dateInMillis, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateInMillis = dateInMillis;
        this.isCompleted = isCompleted;
    }

    // Getter methods
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getDateInMillis() {
        return dateInMillis;
    }

    // Setter methods for updating task data
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDateInMillis(long dateInMillis) {
        this.dateInMillis = dateInMillis;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
