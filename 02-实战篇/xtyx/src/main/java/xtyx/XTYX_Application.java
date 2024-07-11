package xtyx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("xtyx.mapper")
@SpringBootApplication
public class XTYX_Application {

    public static void main(String[] args) {
        SpringApplication.run(XTYX_Application.class, args);
    }

}
