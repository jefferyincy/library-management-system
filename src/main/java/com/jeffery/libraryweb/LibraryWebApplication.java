package com.jeffery.libraryweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jeffery.libraryweb", "service"})
public class LibraryWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryWebApplication.class, args);
    }
}
