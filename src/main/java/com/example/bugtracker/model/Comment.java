package com.example.bugtracker.model;

import java.time.LocalDateTime;

public class Comment {
    private Long id;
    private String author; // username
    private String text;
    private LocalDateTime createdAt;

    public Comment() {
    }

    public Comment(Long id, String author, String text) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
