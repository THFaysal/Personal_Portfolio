package com.thfaysal.portfolio.cms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/** Ensure GridFsTemplate is available even if a Spring Boot version doesn't auto-create one. */
@Configuration
public class GridFsConfig {
    @Bean
    @ConditionalOnMissingBean(GridFsTemplate.class)
    public GridFsTemplate gridFsTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
        return new GridFsTemplate(factory, converter);
    }
}
