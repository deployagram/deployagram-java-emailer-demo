package com.deployagram.demo.emailer;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class RejectInvalidEmailRequestTest {

    private static final String BASE_URI = "http://localhost:8080";
    private static final String EMAIL_PATH = "/Emailer/email";
    private static final String MALFORMED_JSON = "{\"email\": \"eddie@eddie.com\"";

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
    void rejectsAnEmailThatIsNotValidJson() {
        whenMalformedJsonIsPosted();

        thenTheResponseIsBadRequest();
    }

    @Test
    void rejectsAnEmailRequestThatIsNotAPost() {
        whenTheEmailPathIsRequestedWithGet();

        thenTheResponseIsMethodNotAllowed();
    }

    private void whenMalformedJsonIsPosted() {
        response = given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(MALFORMED_JSON)
                .post(EMAIL_PATH);
    }

    private void whenTheEmailPathIsRequestedWithGet() {
        response = given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .get(EMAIL_PATH);
    }

    private void thenTheResponseIsBadRequest() {
        response.then().statusCode(400);
    }

    private void thenTheResponseIsMethodNotAllowed() {
        response.then().statusCode(405);
    }
}
