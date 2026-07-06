package com.hero.middleware.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

}
