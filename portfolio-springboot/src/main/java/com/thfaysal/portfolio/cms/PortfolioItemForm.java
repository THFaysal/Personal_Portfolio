package com.thfaysal.portfolio.cms;

import jakarta.validation.constraints.*;

public class PortfolioItemForm {
    @NotNull private ContentType type = ContentType.PROJECT;
    @NotBlank @Size(max=115) private String title = "";
    @Size(max=65) private String category = "";
    @NotBlank @Size(max=700) private String description = "";
    @Size(max=35) private String status = "";
    @Size(max=90) private String provider = "";
    @Size(max=35) private String dateLabel = "";
    @Size(max=180) private String tags = "";
    @Pattern(regexp="|https://[^\\s]+",message="Use an https:// link or leave blank")
    @Size(max=400) private String externalUrl = "";
    private boolean published = true;
    private boolean featured = true;
    @Min(0) @Max(9999) private int sortOrder = 100;
    private boolean removeImage;
    public ContentType getType(){return type;}
    public void setType(ContentType type){this.type=type;}
    public String getTitle(){return title;}
    public void setTitle(String title){this.title=title;}
    public String getCategory(){return category;}
    public void setCategory(String category){this.category=category;}
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}
    public String getStatus(){return status;}
    public void setStatus(String status){this.status=status;}
    public String getProvider(){return provider;}
    public void setProvider(String provider){this.provider=provider;}
    public String getDateLabel(){return dateLabel;}
    public void setDateLabel(String dateLabel){this.dateLabel=dateLabel;}
    public String getTags(){return tags;}
    public void setTags(String tags){this.tags=tags;}
    public String getExternalUrl(){return externalUrl;}
    public void setExternalUrl(String externalUrl){this.externalUrl=externalUrl;}
    public boolean isPublished(){return published;}
    public void setPublished(boolean published){this.published=published;}
    public boolean isFeatured(){return featured;}
    public void setFeatured(boolean featured){this.featured=featured;}
    public int getSortOrder(){return sortOrder;}
    public void setSortOrder(int sortOrder){this.sortOrder=sortOrder;}
    public boolean isRemoveImage(){return removeImage;}
    public void setRemoveImage(boolean removeImage){this.removeImage=removeImage;}

    public static PortfolioItemForm from(PortfolioItem item) {
        PortfolioItemForm form = new PortfolioItemForm();
        form.type = item.getType(); form.title = item.getTitle(); form.category = item.getCategory();
        form.description = item.getDescription(); form.status = item.getStatus();
        form.provider = item.getProvider(); form.dateLabel = item.getDateLabel();
        form.tags = item.getTags(); form.externalUrl = item.getExternalUrl();
        form.published = item.isPublished(); form.featured = item.isFeatured();
        form.sortOrder = item.getSortOrder(); return form;
    }
}
