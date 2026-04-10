package com.example.readys7project.global.exception.domain;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.ServiceException;

public class AdminException extends ServiceException {
    public AdminException(ErrorCode errorCode) {
        super(errorCode);
    }
}
