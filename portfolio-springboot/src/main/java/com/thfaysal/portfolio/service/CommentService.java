package com.thfaysal.portfolio.service;

import com.thfaysal.portfolio.dto.CommentRequest;
import com.thfaysal.portfolio.dto.CommentResponse;
import com.thfaysal.portfolio.model.PortfolioComment;
import com.thfaysal.portfolio.repository.CommentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository repository;

    public CommentService(CommentRepository repository) {
        this.repository = repository;
    }

    // Expiring, size-limited in-memory cache: avoids querying MongoDB on every page visit.
    @Cacheable(cacheNames = "recentComments", key = "'latest'")
    public List<CommentResponse> recent() {
        return repository.findTop12ByOrderByCreatedAtDesc().stream()
                .map(CommentResponse::from)
                .toList();
    }

    // Clear the cached feed after a successful write so new comments appear immediately.
    @CacheEvict(cacheNames = "recentComments", allEntries = true)
    public PortfolioComment create(CommentRequest request) {
        String name = request.name().trim().replaceAll("\\s+", " ");
        String text = request.message().trim();
        return repository.save(new PortfolioComment(name, text));
    }
}
