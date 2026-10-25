package com.thfaysal.portfolio.cms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/** Thin MongoDB-backed CMS; first-run data mirrors the previous static portfolio. */
@Service
public class PortfolioContentService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioContentService.class);
    private static final String SITE_ID = "site";
    private final PortfolioItemRepository items;
    private final PortfolioSettingsRepository settingsRepo;
    private final MediaStore media;

    /** JavaBean getters keep Thymeleaf property resolution consistent. */
    public static final class PublicView {
        private final List<PortfolioItem> featuredProjects;
        private final List<PortfolioItem> moreProjects;
        private final List<PortfolioItem> certificates;
        private final List<PortfolioItem> courses;
        private final List<PortfolioItem> achievements;
        private final String profileImageUrl;
        public PublicView(List<PortfolioItem> featuredProjects, List<PortfolioItem> moreProjects,
                          List<PortfolioItem> certificates, List<PortfolioItem> courses,
                          List<PortfolioItem> achievements, String profileImageUrl) {
            this.featuredProjects=featuredProjects; this.moreProjects=moreProjects;
            this.certificates=certificates; this.courses=courses; this.achievements=achievements;
            this.profileImageUrl=profileImageUrl;
        }
        public List<PortfolioItem> getFeaturedProjects(){return featuredProjects;}
        public List<PortfolioItem> getMoreProjects(){return moreProjects;}
        public List<PortfolioItem> getCertificates(){return certificates;}
        public List<PortfolioItem> getCourses(){return courses;}
        public List<PortfolioItem> getAchievements(){return achievements;}
        public String getProfileImageUrl(){return profileImageUrl;}
    }

    public PortfolioContentService(PortfolioItemRepository items, PortfolioSettingsRepository settingsRepo,
                                   MediaStore media) {
        this.items = items; this.settingsRepo = settingsRepo; this.media = media;
    }

    @Cacheable(cacheNames="publishedPortfolio", key="'home'")
    public PublicView publicView() {
        try {
            seedOnce();
            List<PortfolioItem> all = items.findAll().stream().filter(PortfolioItem::isPublished)
                    .sorted(Comparator.comparingInt(PortfolioItem::getSortOrder)
                            .thenComparing(item -> item.getTitle() == null ? "" : item.getTitle())).toList();
            return toView(all, settings());
        } catch (DataAccessException ex) {
            // The public homepage stays readable if a free DB cluster is waking up.
            log.warn("Portfolio CMS database temporarily unavailable; showing bundled original content.");
            return toView(seedContent(), new PortfolioSettings());
        }
    }

    private PublicView toView(List<PortfolioItem> all, PortfolioSettings settings) {
        List<PortfolioItem> featured = all.stream().filter(i -> i.getType()==ContentType.PROJECT && i.isFeatured()).toList();
        List<PortfolioItem> more = all.stream().filter(i -> i.getType()==ContentType.PROJECT && !i.isFeatured()).toList();
        List<PortfolioItem> certs = all.stream().filter(i -> i.getType()==ContentType.CERTIFICATE).toList();
        List<PortfolioItem> courses = all.stream().filter(i -> i.getType()==ContentType.COURSE).toList();
        List<PortfolioItem> achievements = all.stream().filter(i -> i.getType()==ContentType.ACHIEVEMENT).toList();
        String image = settings.getProfileImageId() == null
                ? "/assets/images/profile_thf.jpeg" : "/media/" + settings.getProfileImageId();
        return new PublicView(featured, more, certs, courses, achievements, image);
    }

    public synchronized void seedOnce() {
        PortfolioSettings settings = settings();
        if (settings.isSeeded()) return;
        for (PortfolioItem entry : seedContent()) {
            if (!items.existsById(entry.getId())) items.save(entry);
        }
        settings.setSeeded(true);
        settingsRepo.save(settings);
    }

    public PortfolioSettings settings() {
        return settingsRepo.findById(SITE_ID).orElseGet(PortfolioSettings::new);
    }

    public List<PortfolioItem> allForAdmin() {
        seedOnce();
        return items.findAll().stream().sorted(Comparator.comparing((PortfolioItem i) -> i.getType().ordinal())
                .thenComparingInt(PortfolioItem::getSortOrder)).toList();
    }

    public Optional<PortfolioItem> find(String id) { return items.findById(id); }

    @CacheEvict(cacheNames="publishedPortfolio", allEntries=true)
    public PortfolioItem save(String id, PortfolioItemForm form, MultipartFile image) throws IOException {
        PortfolioItem entry = (id == null || id.isBlank()) ? new PortfolioItem()
                : items.findById(id).orElseThrow(() -> new IllegalArgumentException("Content item not found."));
        String oldMedia = entry.getImageId();
        String newMedia = null;
        if (image != null && !image.isEmpty()) newMedia = media.save(image, false);
        try {
            // A stored record's content type cannot be changed accidentally by form tampering.
            if (id == null || id.isBlank()) entry.setType(form.getType());
            entry.setTitle(form.getTitle().trim());
            entry.setCategory(safe(form.getCategory())); entry.setDescription(form.getDescription().trim());
            entry.setStatus(safe(form.getStatus())); entry.setProvider(safe(form.getProvider()));
            entry.setDateLabel(safe(form.getDateLabel())); entry.setTags(safe(form.getTags()));
            entry.setExternalUrl(safe(form.getExternalUrl()));
            entry.setPublished(form.isPublished()); entry.setFeatured(form.isFeatured());
            entry.setSortOrder(form.getSortOrder()); entry.setUpdatedAt(Instant.now());
            if (newMedia != null) { entry.setImageId(newMedia); entry.setAssetPath(null); }
            else if (form.isRemoveImage()) { entry.setImageId(null); entry.setAssetPath(null); }
            PortfolioItem saved = items.save(entry);
            if (oldMedia != null && !oldMedia.equals(saved.getImageId())) media.deleteQuietly(oldMedia);
            return saved;
        } catch (RuntimeException exception) {
            if (newMedia != null) media.deleteQuietly(newMedia);
            throw exception;
        }
    }

    @CacheEvict(cacheNames="publishedPortfolio", allEntries=true)
    public void delete(String id) {
        PortfolioItem entry = items.findById(id).orElseThrow(() -> new IllegalArgumentException("Item not found."));
        items.delete(entry);
        if (entry.getImageId() != null) media.deleteQuietly(entry.getImageId());
    }

    @CacheEvict(cacheNames="publishedPortfolio", allEntries=true)
    public void uploadProfile(MultipartFile image) throws IOException {
        String newId = media.save(image, false);
        PortfolioSettings settings = settings();
        String previous = settings.getProfileImageId();
        try { settings.setProfileImageId(newId); settingsRepo.save(settings); }
        catch (RuntimeException ex) { media.deleteQuietly(newId); throw ex; }
        if (previous != null) media.deleteQuietly(previous);
    }

    @CacheEvict(cacheNames="publishedPortfolio", allEntries=true)
    public void resetProfile() {
        PortfolioSettings settings = settings();
        String previous = settings.getProfileImageId();
        settings.setProfileImageId(null); settingsRepo.save(settings);
        if (previous != null) media.deleteQuietly(previous);
    }

    public void uploadCv(MultipartFile file) throws IOException {
        String newId = media.save(file, true);
        PortfolioSettings settings = settings();
        String previous = settings.getCvFileId();
        try { settings.setCvFileId(newId); settingsRepo.save(settings); }
        catch (RuntimeException ex) { media.deleteQuietly(newId); throw ex; }
        if (previous != null) media.deleteQuietly(previous);
    }

    public void resetCv() {
        PortfolioSettings settings = settings();
        String previous = settings.getCvFileId();
        settings.setCvFileId(null); settingsRepo.save(settings);
        if (previous != null) media.deleteQuietly(previous);
    }

    public boolean isPublicImage(String id) {
        if (id == null) return false;
        if (id.equals(settings().getProfileImageId())) return true;
        return items.existsByImageIdAndPublishedTrue(id);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }

    private static PortfolioItem item(String id, ContentType type, int order, boolean featured,
                                      String title, String category, String status, String description,
                                      String tags, String provider, String date, String asset, String visual) {
        PortfolioItem i = new PortfolioItem();
        i.setId(id); i.setType(type); i.setSortOrder(order); i.setFeatured(featured);
        i.setTitle(title); i.setCategory(category); i.setStatus(status); i.setDescription(description);
        i.setTags(tags); i.setProvider(provider); i.setDateLabel(date);
        i.setAssetPath(asset); i.setVisualType(visual); return i;
    }

    /** Preserves the six existing projects, three certificate cards, and two completed labs. */
    public static List<PortfolioItem> seedContent() {
        return List.of(
            item("seed-sdn",ContentType.PROJECT,10,true,"SDN Traffic Rerouting & Link Recovery", "NETWORK ENGINEERING","Completed",
                    "Designed an SDN campus-network scenario to detect link failures and automatically redirect traffic across redundant paths.",
                    "GNS3, Faucet, OpenFlow, Open vSwitch","","",null,"sdn"),
            item("seed-bakir",ContentType.PROJECT,20,true,"Bakir Khata - Loan Management","WEB APPLICATION","Completed",
                    "Developed a Spring Boot platform for loan records, installments, payments, and repayment status with relational and document storage.",
                    "Spring Boot, MySQL, MongoDB, JPA","","",null,"finance"),
            item("seed-enterprise",ContentType.PROJECT,30,true,"Enterprise Routing & Switching","INFRASTRUCTURE","Lab work",
                    "Built and troubleshot Cisco routing topologies, tested connectivity, and explored routing protocols and traffic behavior.",
                    "Cisco, GNS3, Wireshark","","",null,"routing"),
            item("seed-reedkeep",ContentType.PROJECT,40,true,"ReedKeep - Personal Library","SOFTWARE PROJECT","Completed & Active",
                    "Created a personal library application with Java, Spring Boot and MongoDB to manage an organized book collection.",
                    "Java, Spring Boot, MongoDB","","",null,"library"),
            item("seed-foliocircle",ContentType.PROJECT,50,false,"FolioCircle","SOFTWARE PROJECT","In Progress",
                    "An expanded personal-library and community platform under development with Java, Spring Boot and MongoDB.",
                    "Java, Spring Boot, MongoDB","","",null,"generic"),
            item("seed-website",ContentType.PROJECT,60,false,"Portfolio & Guestbook","THIS WEBSITE","Active",
                    "A Spring Boot site with a MongoDB-backed guestbook, private inbox, downloadable CV, caching and email notifications.",
                    "Spring Boot, MongoDB, Caffeine","","",null,"generic"),
            item("seed-cpte",ContentType.CERTIFICATE,10,true,"C)PTE: Certified Penetration Testing Engineer","Professional certification","Completed",
                    "Certificate issue date: 06/10/2026.","","mile2 Cybersecurity Certifications","2026",
                    "/assets/images/certificates/mile2-cpte.png","generic"),
            item("seed-cuet",ContentType.CERTIFICATE,20,true,"Applied Penetration Testing Engineer: Hands-on","Hands-on training","Completed",
                    "87 hours of training, April 7 - May 18, 2026.","","EDGE-CSE CUET DST Project","May 2026",
                    "/assets/images/certificates/edge-cuet-applied-pentesting.jpg","generic"),
            item("seed-wings",ContentType.CERTIFICATE,30,true,"Applied AI Deep Learning with Python for Data Analysis","Course completion","Completed",
                    "Online research-led, non-credit training completed August 10, 2026.","","WINGS Institute","Aug 2026",
                    "/assets/images/certificates/wings-applied-ai.jpg","generic"),
            item("seed-blue",ContentType.COURSE,10,true,"Blue","TRYHACKME / BLUE","Completed",
                    "Windows and SMB vulnerability-focused security lab.","","TryHackMe","",null,"generic"),
            item("seed-vulnversity",ContentType.COURSE,20,true,"Vulnversity","TRYHACKME / VULNVERSITY","Completed",
                    "Reconnaissance, web enumeration, and privilege-escalation practice.","","TryHackMe","",null,"generic")
        );
    }
}
