package com.jonnoyip.urlshortener.controller;

import com.jonnoyip.urlshortener.model.dto.ShortenUrlRequest;
import com.jonnoyip.urlshortener.model.dto.ShortenUrlResponse;
import com.jonnoyip.urlshortener.service.UrlShorteningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UrlController {

    private final UrlShorteningService urlShorteningService;

    public UrlController(UrlShorteningService urlShorteningService) {
        this.urlShorteningService = urlShorteningService;
    }

    @PostMapping("/shortenUrl")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        ShortenUrlResponse response = urlShorteningService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

