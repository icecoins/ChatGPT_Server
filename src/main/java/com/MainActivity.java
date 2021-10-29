package com;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mapper")
public class MainActivity {
    public static void main(String[] args) {
        SpringApplication.run(MainActivity.class, args);
    }
}
