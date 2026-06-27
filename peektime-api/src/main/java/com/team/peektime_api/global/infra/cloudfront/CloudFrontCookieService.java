package com.team.peektime_api.global.infra.cloudfront;

import com.team.peektime_api.global.infra.cloudfront.dto.SignedCookieResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class CloudFrontCookieService {

    private static final Duration COOKIE_TTL = Duration.ofDays(7);
    private static final CloudFrontUtilities UTILS = CloudFrontUtilities.create();

    @Value("${cloud.aws.cloudfront.domain}")
    private String domain;

    @Value("${cloud.aws.cloudfront.key-pair-id}")
    private String keyPairId;

    @Value("${cloud.aws.cloudfront.private-key}")
    private String privateKeyPem;

    public SignedCookieResponse issueCookies() {
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new IllegalStateException("CLOUDFRONT_PRIVATE_KEY가 설정되지 않았습니다. application-local.yml 또는 환경변수를 확인하세요.");
        }
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPem);
            Instant expiresAt = Instant.now().plus(COOKIE_TTL);
            String resourceUrl = "https://" + domain + "/*";

            CookiesForCustomPolicy cookies = UTILS.getCookiesForCustomPolicy(
                    CustomSignerRequest.builder()
                            .resourceUrl(resourceUrl)
                            .privateKey(privateKey)
                            .keyPairId(keyPairId)
                            .expirationDate(expiresAt)
                            .build()
            );

            return SignedCookieResponse.of(
                    cookies.policyHeaderValue(),
                    cookies.signatureHeaderValue(),
                    cookies.keyPairIdHeaderValue(),
                    expiresAt
            );
        } catch (Exception e) {
            log.error("CloudFront signed cookie 발급 실패", e);
            throw new RuntimeException("CloudFront 쿠키 발급에 실패했습니다", e);
        }
    }

    public String buildUrl(String objectKey) {
        return "https://" + domain + "/" + objectKey;
    }

    // env var에 저장된 PEM은 개행이 \n 리터럴로 들어올 수 있어 치환 후 파싱
    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("[\\r\\n\\s]+", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }
}
