package com.lebens_rhythmus;

import org.springframework.boot.SpringApplication;

public class TestLebensRhythmusApplication {

    public static void main(String[] args) {
        SpringApplication.from(LebensRhythmusApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
