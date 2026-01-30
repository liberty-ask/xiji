package com.xiji;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 玺记系统启动类
 * @author liberty
 */
@SpringBootApplication
@MapperScan("com.xiji.mapper")
@ServletComponentScan("com.xiji.filter")
public class XijiApplication {

    public static void main(String[] args) {
        SpringApplication.run(XijiApplication.class, args);
    }

}

