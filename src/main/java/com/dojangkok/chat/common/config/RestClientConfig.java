package com.dojangkok.chat.common.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${app.main-server.url}")
    private String mainServerUrl;

    @Value("${app.main-server.api-key}")
    private String internalApiKey;

    @Bean
    public HttpComponentsClientHttpRequestFactory mainServerRequestFactory() {
        // 타임아웃 설정 객체 생성 (연결 3초, 응답 대기 5초)
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))
                .setSocketTimeout(Timeout.ofSeconds(5))
                .build();

        // 커넥션 매니저 생성 (타임아웃 설정 장착)
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(50)
                .setDefaultConnectionConfig(connectionConfig) //️ 타임아웃 주입
                .build();

        // HttpClient 생성 및 매니저 장착
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // Spring 용 RequestFactory로 변환
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout(Duration.ofSeconds(2)); // 커넥션 빌릴 경우 대기 시간 설정
        return factory;
    }

    @Bean
    public RestClient mainServerRestClient(HttpComponentsClientHttpRequestFactory mainServerRequestFactory) {
        // 공통 URL과 Header 포함 RestClient 반환
        return RestClient.builder()
                .baseUrl(mainServerUrl)
                .requestFactory(mainServerRequestFactory)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }
}
