package com.bcbsma.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@EnableAspectJAutoProxy
@ComponentScan(basePackages = { "org.openapitools", "com.bcbsma.api" })
public class SpringBootApp {

    public static void main(String[] args) throws Exception {
    	new SpringApplication(SpringBootApp.class).run(args);
    }


}
