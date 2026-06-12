package com.swapmarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.swapmarket.mapper")
@EnableScheduling
public class SwapMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwapMarketApplication.class, args);
    }
}
