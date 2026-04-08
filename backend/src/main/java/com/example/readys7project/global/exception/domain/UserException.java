package com.example.readys7project.global.exception.domain;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.ServiceException;

public class UserException extends ServiceException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
