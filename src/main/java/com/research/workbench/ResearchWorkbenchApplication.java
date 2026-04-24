package com.research.workbench;

import com.research.workbench.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class ResearchWorkbenchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResearchWorkbenchApplication.class, args);
    }
}
