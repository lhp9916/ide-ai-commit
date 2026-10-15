package com.peng.aicommit;

/**
 * 携带用户可读中文说明的异常，最终会原样展示在通知气泡中。
 */
public class AiCommitException extends RuntimeException {
    public AiCommitException(String message) {
        super(message);
    }

    public AiCommitException(String message, Throwable cause) {
        super(message, cause);
    }
}
