package com.thfaysal.portfolio.cms;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Controller
public class PublicMediaController {
    private final PortfolioContentService content;
    private final MediaStore media;
    public PublicMediaController(PortfolioContentService content, MediaStore media) {
        this.content=content; this.media=media;
    }

    @GetMapping("/cv/current")
    public ResponseEntity<Resource> downloadCurrentCv() {
        try {
            String id = content.settings().getCvFileId();
            if (id != null) {
                Optional<MediaStore.FileData> data = media.get(id);
                if (data.isPresent() && data.get().mimeType().equals("application/pdf")) {
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                            .contentLength(data.get().file().getLength())
                            .cacheControl(CacheControl.noStore())
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Tanvirul_Hoque_Faysal_CV.pdf")
                            .body(data.get().resource());
                }
            }
        } catch (DataAccessException e) { /* Serve bundled CV when database is temporarily unavailable. */ }
        ClassPathResource bundled = new ClassPathResource("static/cv/Tanvirul_Hoque_Faysal_Corporate_IT_CV.pdf");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Tanvirul_Hoque_Faysal_CV.pdf")
                .body(bundled);
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<Resource> publicImage(@PathVariable String id) {
        if (!content.isPublicImage(id)) return ResponseEntity.notFound().build();
        return mediaResponse(id, false);
    }

    @GetMapping("/admin/media/{id}")
    public ResponseEntity<Resource> adminImage(@PathVariable String id) {
        return mediaResponse(id, true);
    }

    private ResponseEntity<Resource> mediaResponse(String id, boolean privateAdmin) {
        Optional<MediaStore.FileData> file = media.get(id);
        if (file.isEmpty() || file.get().mimeType().equals("application/pdf")) return ResponseEntity.notFound().build();
        MediaStore.FileData data = file.get();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(data.mimeType()))
                .contentLength(data.file().getLength())
                .cacheControl(privateAdmin ? CacheControl.noStore() : CacheControl.maxAge(30, TimeUnit.MINUTES).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(data.resource());
    }
}
