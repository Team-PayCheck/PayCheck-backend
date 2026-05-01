package com.example.paycheck.common.exception;

import lombok.Getter;

@Getter
public class FileUploadException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public FileUploadException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public FileUploadException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
