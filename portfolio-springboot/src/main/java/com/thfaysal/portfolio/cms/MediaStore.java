package com.thfaysal.portfolio.cms;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/** Persistent uploads: Render filesystem is ephemeral; GridFS persists in MongoDB Atlas. */
@Service
public class MediaStore {
    private static final Logger log = LoggerFactory.getLogger(MediaStore.class);
    public record FileData(GridFSFile file, GridFsResource resource, String mimeType) { }
    private final GridFsTemplate grid;
    public MediaStore(GridFsTemplate grid) { this.grid = grid; }

    /** Rejects unexpected formats, over-sized uploads, and excessive image dimensions. */
    public String save(MultipartFile upload, boolean pdfOnly) throws IOException {
        if (upload == null || upload.isEmpty()) throw new IllegalArgumentException("Choose a file to upload.");
        long limit = pdfOnly ? 5L * 1024 * 1024 : 2L * 1024 * 1024;
        if (upload.getSize() > limit) throw new IllegalArgumentException(pdfOnly ? "CV exceeds 5 MB." : "Image exceeds 2 MB.");
        byte[] bytes = upload.getBytes();
        if (bytes.length == 0 || bytes.length > limit) throw new IllegalArgumentException("File size is invalid.");
        String mime = fileMime(bytes);
        if (pdfOnly && !mime.equals("application/pdf")) throw new IllegalArgumentException("Upload a PDF CV only.");
        if (!pdfOnly && mime.equals("application/pdf")) throw new IllegalArgumentException("Upload JPG, PNG or WebP images only.");
        if (!pdfOnly && !mime.equals("image/webp")) {
            try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
                BufferedImage image = ImageIO.read(stream);
                if (image == null || image.getWidth() > 6000 || image.getHeight() > 6000
                        || (long) image.getWidth() * image.getHeight() > 20_000_000L) {
                    throw new IllegalArgumentException("Invalid image or resolution exceeds 20 megapixels.");
                }
            }
        }
        String ext = switch (mime) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            default -> "webp";
        };
        String filename = UUID.randomUUID() + "." + ext;
        Document metadata = new Document("contentType", mime).append("ownerUploaded", true);
        ObjectId id = grid.store(new ByteArrayInputStream(bytes), filename, mime, metadata);
        return id.toHexString();
    }

    private static String fileMime(byte[] bytes) {
        if (bytes.length >= 5 && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            return "application/pdf";
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
                new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255)
            return "image/jpeg";
        if (bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        throw new IllegalArgumentException("Unsupported file type. Use JPG, PNG, WebP, or PDF as appropriate.");
    }

    public Optional<FileData> get(String id) {
        if (id == null || !ObjectId.isValid(id)) return Optional.empty();
        GridFSFile found = grid.findOne(Query.query(Criteria.where("_id").is(new ObjectId(id))));
        if (found == null || found.getMetadata() == null || !found.getMetadata().getBoolean("ownerUploaded", false))
            return Optional.empty();
        String mime = found.getMetadata().getString("contentType");
        if (!java.util.Set.of("image/jpeg","image/png","image/webp","application/pdf").contains(mime))
            return Optional.empty();
        return Optional.of(new FileData(found, grid.getResource(found), mime));
    }

    public void deleteQuietly(String id) {
        if (id != null && ObjectId.isValid(id)) {
            try { grid.delete(Query.query(Criteria.where("_id").is(new ObjectId(id)))); }
            catch (RuntimeException ex) { log.warn("Unable to delete old GridFS asset; manual cleanup may be required."); }
        }
    }
}
