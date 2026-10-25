package com.thfaysal.portfolio.cms;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaStoreValidationTest {
    private final GridFsTemplate mongo = mock(GridFsTemplate.class);
    private final MediaStore store = new MediaStore(mongo);

    @Test
    void rejectsHtmlDisguisedAsImage() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png",
                "<html><script>alert('bad')</script></html>".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> store.save(file, false));
        verifyNoInteractions(mongo);
    }

    @Test
    void rejectsNonPdfAsCv() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf",
                "this is not a PDF".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> store.save(file, true));
        verifyNoInteractions(mongo);
    }

    @Test
    void rejectsLargeImage() {
        byte[] huge = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", huge);
        assertThrows(IllegalArgumentException.class, () -> store.save(file, false));
        verifyNoInteractions(mongo);
    }
}
