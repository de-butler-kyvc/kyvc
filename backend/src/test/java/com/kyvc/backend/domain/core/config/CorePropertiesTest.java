package com.kyvc.backend.domain.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorePropertiesTest {

    @Test
    void coreTimeoutSeconds_useDefaultValues() {
        CoreProperties properties = new CoreProperties();

        assertThat(properties.resolvedConnectTimeoutSeconds()).isEqualTo(10);
        assertThat(properties.resolvedReadTimeoutSeconds()).isEqualTo(60);
        assertThat(properties.resolvedAiReviewReadTimeoutSeconds()).isEqualTo(300);
        assertThat(properties.resolvedAiReviewReadTimeoutMillis()).isEqualTo(300_000);
    }

    @Test
    void aiReviewReadTimeoutSeconds_clampsOverMaxValue() {
        CoreProperties properties = new CoreProperties();
        properties.setAiReviewReadTimeoutSeconds("600");

        assertThat(properties.resolvedAiReviewReadTimeoutSeconds()).isEqualTo(300);
    }

    @Test
    void timeoutSeconds_useDefaultWhenInvalidValue() {
        CoreProperties properties = new CoreProperties();
        properties.setConnectTimeoutSeconds("not-number");
        properties.setReadTimeoutSeconds("0");
        properties.setAiReviewReadTimeoutSeconds("-1");

        assertThat(properties.resolvedConnectTimeoutSeconds()).isEqualTo(10);
        assertThat(properties.resolvedReadTimeoutSeconds()).isEqualTo(60);
        assertThat(properties.resolvedAiReviewReadTimeoutSeconds()).isEqualTo(300);
    }

    @Test
    void coreReadTimeoutSeconds_areSeparatedFromAiReviewReadTimeoutSeconds() {
        CoreProperties properties = new CoreProperties();
        properties.setReadTimeoutSeconds("45");
        properties.setAiReviewReadTimeoutSeconds("120");

        assertThat(properties.resolvedReadTimeoutSeconds()).isEqualTo(45);
        assertThat(properties.resolvedAiReviewReadTimeoutSeconds()).isEqualTo(120);
    }
}
