package com.jonnoyip.urlshortener.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String error;
    private String message;
    private String details;
    private LocalDateTime timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, String message, String details, LocalDateTime timestamp) {
        this.error = error;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

