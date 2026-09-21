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
class ReceiveEmailTest {

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
                .baseUri(Environment.PROXY_BASE_URI)
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
