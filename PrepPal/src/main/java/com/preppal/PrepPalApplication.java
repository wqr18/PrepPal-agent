package com.preppal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.preppal.module.**.mapper")
public class PrepPalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrepPalApplication.class, args);
    }
}
