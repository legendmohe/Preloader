package com.legendmohe.preloader;

/**
 * Preload异常
 */
public class PreloadException extends Exception {

    private int mErrorCode;

    public PreloadException(int errorCode, String message) {
        super(message);
        mErrorCode = errorCode;
    }

    public PreloadException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        mErrorCode = errorCode;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
