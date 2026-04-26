package com.skillbridge.user.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> data,
    Object meta
) {
}
