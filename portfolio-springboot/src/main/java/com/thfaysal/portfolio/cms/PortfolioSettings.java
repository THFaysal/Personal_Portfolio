package com.thfaysal.portfolio.cms;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "portfolio_settings")
public class PortfolioSettings {
    @Id private String id = "site";
    private boolean seeded;
    private String profileImageId;
    private String cvFileId;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isSeeded() { return seeded; }
    public void setSeeded(boolean seeded) { this.seeded = seeded; }
    public String getProfileImageId() { return profileImageId; }
    public void setProfileImageId(String profileImageId) { this.profileImageId = profileImageId; }
    public String getCvFileId() { return cvFileId; }
    public void setCvFileId(String cvFileId) { this.cvFileId = cvFileId; }
}
