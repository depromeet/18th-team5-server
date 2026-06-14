package com.team.peektime_api.global.infra.S3;

import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.cache.PresignedUrlCacheRepository;
import com.team.peektime_api.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedUrlCacheRepository presignedUrlCacheRepository;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
    }

    @Test
    void objectKey가_null이면_검증을_건너뛴다() {
        assertThatCode(() -> s3Service.validateObjectExists(null))
                .doesNotThrowAnyException();
    }

    @Test
    void S3에_객체가_존재하면_예외가_발생하지_않는다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder().build());

        assertThatCode(() -> s3Service.validateObjectExists("images/valid-key.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void S3에_객체가_없으면_BusinessException이_발생한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThatThrownBy(() -> s3Service.validateObjectExists("images/invalid-key.jpg"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.S3_OBJECT_NOT_FOUND);
    }

    @Test
    void S3_403_에러는_S3Exception이_그대로_전파된다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

        assertThatThrownBy(() -> s3Service.validateObjectExists("images/some-key.jpg"))
                .isInstanceOf(S3Exception.class);
    }
}
