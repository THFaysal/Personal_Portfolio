package com.thfaysal.portfolio.cms;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PortfolioItemRepository extends MongoRepository<PortfolioItem, String> {
    boolean existsByImageIdAndPublishedTrue(String imageId);
}
