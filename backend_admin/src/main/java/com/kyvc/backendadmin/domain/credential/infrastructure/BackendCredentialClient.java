package com.kyvc.backendadmin.domain.credential.infrastructure;

import com.kyvc.backendadmin.domain.credential.dto.CredentialActionResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Backend API에 VC 재발급과 폐기를 위임하는 Client입니다.
 *
 * <p>backend_admin은 Core 또는 XRPL 로직을 직접 처리하지 않고, 검증된 관리자 요청을 Backend API로 전달합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class BackendCredentialClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient backendCredentialRestClient;
    private final BackendCredentialProperties properties;

    /**
     * Backend API에 VC 재발급을 요청합니다.
     *
     * @param credentialId VC ID
     * @param request VC 재발급 요청 정보
     * @param adminId 요청 관리자 ID
     * @return VC 재발급 처리 결과
     */
    public CredentialActionResponse reissueCredential(
            Long credentialId,
            CredentialReissueRequest request,
            Long adminId
    ) {
        return callBackend(
                properties.getReissuePath(),
                credentialId,
                new BackendCredentialActionRequest(adminId, request.reason()),
                ErrorCode.VC_REISSUE_REQUEST_FAILED,
                "REISSUE",
                "VC 재발급 요청이 Backend API에 접수되었습니다."
        );
    }

    /**
     * Backend API에 VC 폐기를 요청합니다.
     *
     * @param credentialId VC ID
     * @param request VC 폐기 요청 정보
     * @param adminId 요청 관리자 ID
     * @return VC 폐기 처리 결과
     */
    public CredentialActionResponse revokeCredential(
            Long credentialId,
            CredentialRevokeRequest request,
            Long adminId
    ) {
        return callBackend(
                properties.getRevokePath(),
                credentialId,
                new BackendCredentialActionRequest(adminId, request.reason()),
                ErrorCode.VC_REVOKE_REQUEST_FAILED,
                "REVOKE",
                "VC 폐기 요청이 Backend API에 접수되었습니다."
        );
    }

    private CredentialActionResponse callBackend(
            String path,
            Long credentialId,
            BackendCredentialActionRequest request,
            ErrorCode failureCode,
            String action,
            String fallbackMessage
    ) {
        try {
            CommonResponse<CredentialActionResponse> response = backendCredentialRestClient.post()
                    .uri(path, Map.of("credentialId", credentialId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(this::applyInternalApiKey)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success()) {
                throw new ApiException(failureCode, response == null ? fallbackMessage : response.message());
            }
            CredentialActionResponse data = response.data();
            return data == null ? CredentialActionResponse.accepted(credentialId, action, fallbackMessage) : data;
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new ApiException(failureCode, "Backend Credential API 호출이 실패했습니다.", exception);
        } catch (RestClientException exception) {
            throw new ApiException(ErrorCode.BACKEND_CALL_FAILED, "Backend Credential API 호출 중 오류가 발생했습니다.", exception);
        }
    }

    private void applyInternalApiKey(HttpHeaders headers) {
        if (StringUtils.hasText(properties.getInternalApiKey())) {
            headers.set(INTERNAL_API_KEY_HEADER, properties.getInternalApiKey().trim());
        }
    }

    /**
     * Backend에 전달하는 관리자 Credential 액션 요청입니다.
     *
     * @param adminId 요청 관리자 ID
     * @param reason 요청 사유
     */
    private record BackendCredentialActionRequest(
            Long adminId,
            String reason
    ) {
    }
}
