package com.deployagram.demo.emailer;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class EmailerApplication {

    private static final int PORT = 8080;
    private static final String EMAIL_PATH = "/Emailer/email";

    private final HttpServer server;

    public EmailerApplication() {
        server = createServer();
        server.createContext(EMAIL_PATH, new EmailHandler());
    }

    public static void main(String[] args) {
        new EmailerApplication().start();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private static HttpServer createServer() {
        try {
            return HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to listen on port " + PORT, e);
        }
    }
}
