package com.deployagram.demo.emailer;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class ReceiveEmailTest {

    private static final String BASE_URI = "http://localhost:8080";
    private static final String EMAIL_PATH = "/Emailer/email";
    private static final String EMAIL_JSON = """
            {
              "email": "eddie@eddie.com",
              "subject": "Test email subject in here",
              "body": "Simple text email body"
            }
            """;

    private static EmailerApplication application;

    private Response response;

    @BeforeAll
    static void startApplication() {
        application = new EmailerApplication();
        application.start();
    }

    @AfterAll
    static void stopApplication() {
        application.stop();
    }

    @Test
    void acceptsAnEmailRequest() {
        whenTheEmailIsPosted();

        thenTheResponseIsNoContent();
        thenTheResponseIsJson();
    }

    private void whenTheEmailIsPosted() {
        response = given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(EMAIL_JSON)
                .post(EMAIL_PATH);
    }

    private void thenTheResponseIsNoContent() {
        response.then().statusCode(204);
    }

    private void thenTheResponseIsJson() {
        response.then().contentType(ContentType.JSON);
    }
}
