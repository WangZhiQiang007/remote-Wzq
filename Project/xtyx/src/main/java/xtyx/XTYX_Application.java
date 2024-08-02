package xtyx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("xtyx.mapper")
@SpringBootApplication
public class  XTYX_Application {

    public static void main(String[] args) {
        //写一个启动时的队形
        System.out.println("------------------------*******************---------------------");
        System.out.println("|                          欢迎使用星探优选                          |");
        System.out.println("|                             启动成功                             |");
        System.out.println("------------------------*******************----------------------");
        System.out.println();
        SpringApplication.run(XTYX_Application.class, args);
    }
    
}
