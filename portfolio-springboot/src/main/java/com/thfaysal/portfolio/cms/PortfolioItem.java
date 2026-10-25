package com.thfaysal.portfolio.cms;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Content managed by the site owner. All user-controlled text is escaped by Thymeleaf. */
@Document(collection = "portfolio_content")
public class PortfolioItem {
    @Id private String id;
    private ContentType type;
    private String title = "";
    private String category = "";
    private String description = "";
    private String status = "";
    private String provider = "";
    private String dateLabel = "";
    private String tags = "";
    private String externalUrl = "";
    private String imageId;
    private String assetPath;  // bundled, previously verified certificate image
    private String visualType = "generic"; // preserves four existing project illustrations
    private boolean published = true;
    private boolean featured = true;
    private int sortOrder = 100;
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ContentType getType() { return type; }
    public void setType(ContentType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }
    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String assetPath) { this.assetPath = assetPath; }
    public String getVisualType() { return visualType; }
    public void setVisualType(String visualType) { this.visualType = visualType; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim).filter(t -> !t.isEmpty()).limit(8).toList();
    }
    public String getMediaUrl() {
        if (imageId != null && !imageId.isBlank()) return "/media/" + imageId;
        return assetPath;
    }
    public boolean isImage() {
        String url = getMediaUrl();
        return url != null && !url.toLowerCase().endsWith(".pdf");
    }
}
