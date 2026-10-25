package com.thfaysal.portfolio.service;

import com.thfaysal.portfolio.dto.CommentRequest;
import com.thfaysal.portfolio.model.PortfolioComment;
import com.thfaysal.portfolio.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    CommentRepository repository;
    private CommentService service;

    @BeforeEach
    void init() {
        service = new CommentService(repository);
    }

    @Test
    void savedCommentIsTrimmedBeforePersistence() {
        when(repository.save(any(PortfolioComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PortfolioComment saved = service.create(new CommentRequest("  A   Visitor  ", "  Great project!  ", ""));
        assertEquals("A Visitor", saved.getName());
        assertEquals("Great project!", saved.getMessage());
    }

    @Test
    void feedIncludesPublicFieldsOnly() {
        PortfolioComment stored = new PortfolioComment("A Visitor", "Clear network diagram");
        when(repository.findTop12ByOrderByCreatedAtDesc()).thenReturn(List.of(stored));
        var result = service.recent();
        assertEquals(1, result.size());
        assertEquals("A Visitor", result.get(0).name());
        assertEquals("Clear network diagram", result.get(0).message());
    }
}
