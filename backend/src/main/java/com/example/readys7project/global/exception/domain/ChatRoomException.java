package com.example.readys7project.global.exception.domain;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.ServiceException;

public class ChatRoomException extends ServiceException {
    public ChatRoomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
