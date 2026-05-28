package com.empik.coupon.infrastructure.config;

import com.empik.coupon.infrastructure.geolocation.GeolocationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
class InfrastructureConfig {

    /**
     * RestClient for the geolocation service with explicit connect and read timeouts.
     *
     * <p>Geolocation is on the critical path of every coupon-use request.
     * Without timeouts a degraded ip-api.com would cause server threads to block
     * indefinitely, leading to thread-pool exhaustion under moderate load.
     */
    @Bean
    RestClient geolocationRestClient(GeolocationProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));

        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }
}
