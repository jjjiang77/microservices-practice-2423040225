package com.zjgsu.jby.exception;

/**
 * 唯一约束冲突（如学号重复、同卖家同标题重复发布），映射为 HTTP 409。
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
