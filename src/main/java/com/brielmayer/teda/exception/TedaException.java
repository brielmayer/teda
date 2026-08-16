package com.brielmayer.teda.exception;

public class TedaException extends RuntimeException {

    public TedaException(final String message, final Exception cause) {
        super(message, cause);
    }

    public static TedaExceptionBuilder builder() {
        return new TedaExceptionBuilder();
    }

    public static class TedaExceptionBuilder {

        private final StringBuilder message = new StringBuilder();
        private Exception cause;

        public TedaExceptionBuilder appendMessage() {
            this.message.append(System.lineSeparator());
            return this;
        }

        public TedaExceptionBuilder appendMessage(final String message, final Object... params) {
            this.message.append(System.lineSeparator()).append(String.format(message, params));
            return this;
        }

        public TedaExceptionBuilder cause(final Exception cause) {
            this.cause = cause;
            return this;
        }

        public TedaException build() {
            if (cause != null && !(cause instanceof TedaException) && cause.getMessage() != null) {
                appendMessage("Reason: %s", cause.getMessage());
            }
            return new TedaException(message.toString(), cause);
        }
    }
}
