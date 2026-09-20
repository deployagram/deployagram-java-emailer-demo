package com.deployagram.demo.emailer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class EmailHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EmailHandler.class);
    private static final int NO_CONTENT = 204;
    private static final int BAD_REQUEST = 400;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int NO_RESPONSE_BODY = -1;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (isPost(exchange)) {
                acceptEmail(exchange);
            } else {
                respondWith(METHOD_NOT_ALLOWED, exchange);
            }
        }
    }

    private boolean isPost(HttpExchange exchange) {
        return "POST".equals(exchange.getRequestMethod());
    }

    private void acceptEmail(HttpExchange exchange) throws IOException {
        try {
            logReceipt(readEmail(exchange));
            respondWith(NO_CONTENT, exchange);
        } catch (JacksonException e) {
            LOG.warn("Rejected malformed email request: {}", e.getOriginalMessage());
            respondWith(BAD_REQUEST, exchange);
        }
    }

    private Email readEmail(HttpExchange exchange) throws IOException {
        return objectMapper.readValue(exchange.getRequestBody(), Email.class);
    }

    private void logReceipt(Email email) {
        LOG.info("Received email for {} with subject \"{}\": {}", email.email(), email.subject(), email.body());
    }

    private void respondWith(int statusCode, HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, NO_RESPONSE_BODY);
    }
}
