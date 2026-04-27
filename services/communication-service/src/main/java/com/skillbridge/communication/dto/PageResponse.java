package com.skillbridge.communication.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> data,
    Object meta
) {
}
