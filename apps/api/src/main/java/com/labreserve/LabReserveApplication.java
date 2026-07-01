package com.labreserve;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.labreserve.mapper")
@EnableCaching
public class LabReserveApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabReserveApplication.class, args);
    }
}
