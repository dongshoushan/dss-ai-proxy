package com.dss.test.ai.proxy.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * RestTemplate配置类
 * 根据配置动态决定是否使用代理
 *
 * @author dongshoushan
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final ProxyConfig proxyConfig;

    /**
     * 创建RestTemplate Bean
     * 根据配置动态决定是否使用代理：
     * - 如果配置了proxy-host和proxy-port，则使用代理
     * - 如果未配置代理，则直连目标服务
     *
     * @return RestTemplate实例
     * @author dongshoushan
     */
    @Bean
    public RestTemplate restTemplate() {
        // 禁用SSL证书验证
        disableSSLVerification();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 动态判断是否使用代理
        if (proxyConfig.isProxyEnabled()) {
            log.info("代理已启用: {}:{}", proxyConfig.getProxyHost(), proxyConfig.getProxyPort());
            
            // 设置代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(proxyConfig.getProxyHost(), proxyConfig.getProxyPort()));
            factory.setProxy(proxy);

            // 设置代理认证（如果配置了用户名密码）
            if (proxyConfig.isProxyAuthEnabled()) {
                log.info("代理认证已启用, 用户: {}", proxyConfig.getProxyUsername());
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            proxyConfig.getProxyUsername(),
                            proxyConfig.getProxyPassword().toCharArray()
                        );
                    }
                });
            }
        } else {
            log.info("代理未启用, 将直连目标服务");
        }

        // 设置超时时间
        factory.setConnectTimeout(proxyConfig.getConnectTimeout());
        factory.setReadTimeout(proxyConfig.getReadTimeout());

        RestTemplate restTemplate = new RestTemplate(factory);

        // 设置UTF-8编码的消息转换器
        restTemplate.getMessageConverters()
            .stream()
            .filter(converter -> converter instanceof StringHttpMessageConverter)
            .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));

        return restTemplate;
    }

    /**
     * 禁用SSL证书验证
     * 用于支持通过代理访问HTTPS站点
     *
     * @author dongshoushan
     */
    private void disableSSLVerification() {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // 安装信任所有证书的TrustManager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // 创建不进行主机名验证的HostnameVerifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // 安装不进行主机名验证的HostnameVerifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            log.error("禁用SSL验证失败", e);
            throw new RuntimeException("Failed to disable SSL verification", e);
        }
    }
}
