package com.cohort.api.config;

import com.cohort.api.service.resume.storage.FileStorageStrategy;
import com.cohort.api.service.resume.storage.LocalFileStorageImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportStorageConfig {
    @Bean
    public FileStorageStrategy reportFileStorage(@Value("${app.report-storage.base-dir:./data/reports}") String baseDir) {
        return new LocalFileStorageImpl(baseDir);
    }
}
