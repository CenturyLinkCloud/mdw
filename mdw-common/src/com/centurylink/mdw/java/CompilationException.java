package com.centurylink.mdw.java;

public class CompilationException extends MdwJavaException {

    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
