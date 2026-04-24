package com.example.readys7project.global.exception.domain;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.ServiceException;

public class ReviewException extends ServiceException {

    public ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }
}
