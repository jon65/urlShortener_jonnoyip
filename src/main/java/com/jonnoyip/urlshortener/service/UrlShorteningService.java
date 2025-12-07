package com.jonnoyip.urlshortener.service;

import com.jonnoyip.urlshortener.model.dto.ShortenUrlRequest;
import com.jonnoyip.urlshortener.model.dto.ShortenUrlResponse;
import com.jonnoyip.urlshortener.model.entity.UrlMapping;
import com.jonnoyip.urlshortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;

@Service
@Transactional
public class UrlShorteningService {

    private final UrlMappingRepository urlMappingRepository;

    public UrlShorteningService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    public ShortenUrlResponse createShortUrl(ShortenUrlRequest request) {
        // Extract short code from shortened URL
        String shortCode = extractShortCode(request.getShortenedUrl());
        
        // Validate the redirected link (original URL)
        validateUrl(request.getRedirectedLink());
        
        // Check if short code already exists
        if (urlMappingRepository.existsByShortCode(shortCode)) {
            throw new IllegalArgumentException("Short code already exists: " + shortCode);
        }
        
        // Create and save the URL mapping
        UrlMapping urlMapping = new UrlMapping(shortCode, request.getRedirectedLink());
        urlMapping = urlMappingRepository.save(urlMapping);
        
        // Build response
        ShortenUrlResponse response = new ShortenUrlResponse();
        response.setShortenedUrl(request.getShortenedUrl());
        response.setRedirectedLink(request.getRedirectedLink());
        response.setCreatedAt(urlMapping.getCreatedAt());
        response.setMessage("URL mapping created successfully");
        
        return response;
    }

    private String extractShortCode(String shortenedUrl) {
        if (shortenedUrl == null || shortenedUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Shortened URL cannot be empty");
        }
        
        // Remove protocol and domain, extract the path
        try {
            URL url = new URL(shortenedUrl);
            String path = url.getPath();
            
            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // If path is empty, use the full URL as short code
            if (path.isEmpty()) {
                return shortenedUrl;
            }
            
            return path;
        } catch (Exception e) {
            // If URL parsing fails, treat the entire string as short code
            String code = shortenedUrl.trim();
            if (code.startsWith("/")) {
                code = code.substring(1);
            }
            return code;
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Redirected link cannot be empty");
        }
        
        try {
            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol();
            
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new IllegalArgumentException("URL must use http or https protocol");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }
}

