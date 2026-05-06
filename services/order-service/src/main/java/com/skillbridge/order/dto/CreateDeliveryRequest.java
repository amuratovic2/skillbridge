package com.skillbridge.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDeliveryRequest {

    @NotBlank(message = "Message is required")
    private String message;
    private String fileUrl;
    private String fileName;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}