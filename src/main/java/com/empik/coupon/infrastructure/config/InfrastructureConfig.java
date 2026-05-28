package com.empik.coupon.infrastructure.config;

import com.empik.coupon.infrastructure.geolocation.GeolocationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class InfrastructureConfig {

    @Bean
    RestClient geolocationRestClient(GeolocationProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .build();
    }
}
