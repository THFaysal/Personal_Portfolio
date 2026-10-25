package com.thfaysal.portfolio.cms;

public enum ContentType {
    PROJECT("Projects"),
    CERTIFICATE("Certifications"),
    COURSE("Courses & labs"),
    ACHIEVEMENT("Achievements");

    private final String label;
    ContentType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
