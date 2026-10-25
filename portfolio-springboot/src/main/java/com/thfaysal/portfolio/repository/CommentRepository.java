package com.thfaysal.portfolio.repository;

import com.thfaysal.portfolio.model.PortfolioComment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<PortfolioComment, String> {
    List<PortfolioComment> findTop12ByOrderByCreatedAtDesc();
}
