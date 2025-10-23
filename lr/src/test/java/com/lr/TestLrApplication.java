package com.lr;

import org.springframework.boot.SpringApplication;

public class TestLrApplication {

    public static void main(String[] args) {
        SpringApplication.from(LrApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
