package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan({"com.centurylink.mdw.hub"})
public class MyApplication {

  public static void main(String[] args) {
      try {
        SpringApplication.run(MyApplication.class, args);
      }
      catch (Throwable t) {
          t.printStackTrace();
      }
  }
}
