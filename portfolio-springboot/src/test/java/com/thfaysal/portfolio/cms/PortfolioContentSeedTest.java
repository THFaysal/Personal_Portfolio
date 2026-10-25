package com.thfaysal.portfolio.cms;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PortfolioContentSeedTest {
    @Test
    void preservesExistingProjectsCertificatesAndLabs() {
        List<PortfolioItem> entries = PortfolioContentService.seedContent();
        assertEquals(6, entries.stream().filter(i -> i.getType() == ContentType.PROJECT).count());
        assertEquals(3, entries.stream().filter(i -> i.getType() == ContentType.CERTIFICATE).count());
        assertEquals(2, entries.stream().filter(i -> i.getType() == ContentType.COURSE).count());
        assertTrue(entries.stream().anyMatch(i -> i.getTitle().contains("ReedKeep") && i.isPublished()));
        assertTrue(entries.stream().anyMatch(i -> i.getTitle().equals("FolioCircle") && !i.isFeatured()));
    }

    @Test
    void itemTagsAreSplitForPublicCards() {
        PortfolioItem item = PortfolioContentService.seedContent().get(0);
        assertTrue(item.getTagList().contains("GNS3"));
        assertTrue(item.getTagList().contains("OpenFlow"));
    }
}
