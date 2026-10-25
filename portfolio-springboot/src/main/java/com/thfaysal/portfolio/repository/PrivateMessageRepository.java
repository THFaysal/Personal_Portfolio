package com.thfaysal.portfolio.repository;

import com.thfaysal.portfolio.model.PrivateMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrivateMessageRepository extends MongoRepository<PrivateMessage, String> {
    Page<PrivateMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByReadAtIsNull();
}
