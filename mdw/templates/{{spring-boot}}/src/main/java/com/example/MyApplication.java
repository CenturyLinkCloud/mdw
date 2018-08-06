package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import com.centurylink.mdwdemo.DemoApplication;

@SpringBootApplication
@ServletComponentScan({"com.centurylink.mdw.boot.servlet","com.centurylink.mdw.hub.servlet"})
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}