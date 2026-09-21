package com.deployagram.demo.emailer;

import com.github.deployagram.annotations.junit5.Deployagram;
import com.github.deployagram.annotations.junit5.DeployagramConfig;
import com.github.deployagram.annotations.junit5.DeployagramConfigEntry;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Deployagram(startEnvironment = true, shareHostPorts = {Environment.APP_PORT}, proxyPort = Environment.PROXY_PORT)
@DeployagramConfig({
        @DeployagramConfigEntry(key = "proxy.namesOfSourceApps.Emailer/email", value = "EmailClient"),
        @DeployagramConfigEntry(key = "proxy.proxiedAppNames.Emailer/email", value = "Emailer"),
        @DeployagramConfigEntry(key = "proxy.proxiedApps.Emailer/email", value = Environment.APP_URL_FOR_PROXY + "/Emailer/email"),
})
class RejectInvalidEmailRequestTest {

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
                .baseUri(Environment.PROXY_BASE_URI)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(MALFORMED_JSON)
                .post(EMAIL_PATH);
    }

    private void whenTheEmailPathIsRequestedWithGet() {
        response = given()
                .baseUri(Environment.PROXY_BASE_URI)
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
