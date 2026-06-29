package com.example.jejugilmoa.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public interface BaseCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
