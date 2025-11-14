package com.example.bugtracker.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bug {
    private Long id;
    private String title;
    private String description;
    private String status; // OPEN, IN_PROGRESS, CLOSED
    private String priority; // LOW, MEDIUM, HIGH
    private LocalDateTime createdAt;

    // new fields
    private String assignedTo; // username or free text
    private List<Comment> comments = new ArrayList<>();
    private List<String> attachments = new ArrayList<>(); // stored filenames relative to data/uploads

    public Bug() {
    }

    public Bug(Long id, String title, String description, String priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority == null ? "MEDIUM" : priority;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    // existing getters/setters ...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
}
