package com.empik.coupon.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IpExtractorTest {

    @Mock
    HttpServletRequest request;

    IpExtractor ipExtractor;

    @BeforeEach
    void setUp() {
        ipExtractor = new IpExtractor();
    }

    @Test
    void returnsRemoteAddr_whenNoProxyHeadersPresent() {
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getHeader("Proxy-Client-IP")).willReturn(null);
        given(request.getHeader("WL-Proxy-Client-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn("1.2.3.4");

        assertThat(ipExtractor.extractClientIp(request)).isEqualTo("1.2.3.4");
    }

    @Test
    void prefersXForwardedFor_overRemoteAddr() {
        given(request.getHeader("X-Forwarded-For")).willReturn("5.6.7.8");

        assertThat(ipExtractor.extractClientIp(request)).isEqualTo("5.6.7.8");
    }

    @Test
    void extractsFirstIp_fromXForwardedForChain() {
        given(request.getHeader("X-Forwarded-For")).willReturn("5.6.7.8, 10.0.0.1, 192.168.0.1");

        assertThat(ipExtractor.extractClientIp(request)).isEqualTo("5.6.7.8");
    }

    @Test
    void trimsWhitespace_aroundIpInChain() {
        given(request.getHeader("X-Forwarded-For")).willReturn("  5.6.7.8  , 10.0.0.1");

        assertThat(ipExtractor.extractClientIp(request)).isEqualTo("5.6.7.8");
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "UNKNOWN", "Unknown"})
    void skipsHeader_whenValueIsUnknown(String unknownValue) {
        given(request.getHeader("X-Forwarded-For")).willReturn(unknownValue);
        given(request.getHeader("X-Real-IP")).willReturn("9.9.9.9");

        assertThat(ipExtractor.extractClientIp(request)).isEqualTo("9.9.9.9");
    }

    @Test
    void fallsBackToXRealIp_whenXForwardedForIsBlank() {
        given(request.getHeader("X-Forwarded-For")).willReturn("  ");
        given(request.getHeader("X-Real-IP")).willReturn("9.9.9.9");

        assertThat(ipExtractor.extractClientIp(request)).isEqualTo("9.9.9.9");
    }
}
