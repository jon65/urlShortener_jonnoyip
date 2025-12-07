package com.jonnoyip.urlshortener.model.dto;

import java.time.LocalDateTime;

public class ShortenUrlResponse {

    private String shortenedUrl;
    private String redirectedLink;
    private LocalDateTime createdAt;
    private String message;

    // Constructors
    public ShortenUrlResponse() {
    }

    public ShortenUrlResponse(String shortenedUrl, String redirectedLink, LocalDateTime createdAt) {
        this.shortenedUrl = shortenedUrl;
        this.redirectedLink = redirectedLink;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getShortenedUrl() {
        return shortenedUrl;
    }

    public void setShortenedUrl(String shortenedUrl) {
        this.shortenedUrl = shortenedUrl;
    }

    public String getRedirectedLink() {
        return redirectedLink;
    }

    public void setRedirectedLink(String redirectedLink) {
        this.redirectedLink = redirectedLink;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

