package com.team.peektime_api.global.infra.S3.dto;

public record PresignedUrlResponse(
        String presignedUrl,
        String objectKey
) {}
