package com.skillbridge.gig.dto;

public record PageResponse<T>(
    T data,
    Object meta
) {
}
