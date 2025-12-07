package com.jonnoyip.urlshortener.repository;

import com.jonnoyip.urlshortener.model.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByShortCodeAndIsActiveTrue(String shortCode);

    boolean existsByShortCode(String shortCode);
}

