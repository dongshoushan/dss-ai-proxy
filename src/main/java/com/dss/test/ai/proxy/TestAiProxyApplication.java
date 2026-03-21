package com.dss.test.ai.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI代理应用主类
 *
 * @author dss
 */
@SpringBootApplication
public class TestAiProxyApplication {

    /**
     * 应用主入口
     *
     * @param args 命令行参数
     * @author dongshoushan
     */
    public static void main(String[] args) {
        // 启用系统代理设置
        System.setProperty("java.net.useSystemProxies", "true");

        SpringApplication.run(TestAiProxyApplication.class, args);
    }

}
