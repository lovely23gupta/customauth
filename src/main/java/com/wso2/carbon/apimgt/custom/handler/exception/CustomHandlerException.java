package com.wso2.carbon.apimgt.custom.handler.exception;

public class CustomHandlerException extends Exception {
    
    public CustomHandlerException(String msg, Throwable e) {
        super(msg, e);
    }
}
