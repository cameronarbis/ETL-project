package com.dataplatform.etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.dataplatform.etl", "com.dataplatform.common"})
public class EtlPipelineApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtlPipelineApplication.class, args);
    }
}
