package com.labreserve;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.labreserve.mapper")
public class LabReserveApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabReserveApplication.class, args);
    }
}
