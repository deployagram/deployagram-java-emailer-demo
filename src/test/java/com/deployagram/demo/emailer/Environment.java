package com.deployagram.demo.emailer;

final class Environment {

    static final int APP_PORT = 8080;
    static final int PROXY_PORT = 9080;
    static final String PROXY_BASE_URI = "http://localhost:" + PROXY_PORT;
    static final String APP_URL_FOR_PROXY = "http://host.testcontainers.internal:" + APP_PORT;

    private Environment() {
    }
}
