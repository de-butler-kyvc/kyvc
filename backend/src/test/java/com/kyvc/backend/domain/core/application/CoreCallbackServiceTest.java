package com.kyvc.backend.domain.core.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreCallbackServiceTest {

    private static final String[] REMOVED_CALLBACK_CLASSES = {
            "com.kyvc.backend.domain.core.controller.CoreCallbackController",
            "com.kyvc.backend.domain.core.application.CoreCallbackService",
            "com.kyvc.backend.domain.core.dto.CoreAiReviewCallbackRequest",
            "com.kyvc.backend.domain.core.dto.CoreCallbackResponse",
            "com.kyvc.backend.domain.core.dto.CoreVcIssuanceCallbackRequest",
            "com.kyvc.backend.domain.core.dto.CoreVpVerificationCallbackRequest",
            "com.kyvc.backend.domain.core.dto.CoreXrplTransactionCallbackRequest"
    };

    @Test
    void coreCallbackClasses_areRemovedFromClasspath() {
        for (String className : REMOVED_CALLBACK_CLASSES) {
            assertThatThrownBy(() -> Class.forName(className))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }
}
