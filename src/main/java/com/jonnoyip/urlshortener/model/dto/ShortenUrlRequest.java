package com.jonnoyip.urlshortener.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShortenUrlRequest {

    @NotBlank(message = "Shortened URL is required")
    @Size(max = 255, message = "Shortened URL must not exceed 255 characters")
    private String shortenedUrl;

    @NotBlank(message = "Redirected link is required")
    @Size(max = 2048, message = "Redirected link must not exceed 2048 characters")
    private String redirectedLink;

    // Constructors
    public ShortenUrlRequest() {
    }

    public ShortenUrlRequest(String shortenedUrl, String redirectedLink) {
        this.shortenedUrl = shortenedUrl;
        this.redirectedLink = redirectedLink;
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
}

