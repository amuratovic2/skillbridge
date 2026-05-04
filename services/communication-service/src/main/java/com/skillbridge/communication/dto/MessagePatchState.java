package com.skillbridge.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePatchState {

    @NotBlank
    @Size(max = 2000)
    private String content;

    @NotNull
    private Boolean isRead;
}
