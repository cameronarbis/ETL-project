package com.dataplatform.ingress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.dataplatform.ingress", "com.dataplatform.common"})
public class IngressServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngressServiceApplication.class, args);
    }
}