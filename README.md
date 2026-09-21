# Deployagram Java Emailer Demo

A minimal Java 25 HTTP service, built with no framework, that demonstrates how to instrument a
service test with [Deployagram](https://deployagram.com).

The application plays the part of an **Emailer**: it accepts a request to send an email and logs
it. It does not actually send anything. The interesting part is the test, which is instrumented so
that Deployagram records the HTTP traffic between a caller and the Emailer.

## What it does

The application listens on port `8080` for one request:

```
POST /Emailer/email
Accept: application/json
Content-Type: application/json

{
  "email": "random@anemail.com",
  "subject": "Any subject is valid",
  "body": "Plain text body for an email"
}
```

| Situation                | Response                                           |
|--------------------------|----------------------------------------------------|
| Valid request            | `204 No Content`, `Content-Type: application/json` |
| Body is not valid JSON   | `400 Bad Request`                                  |
| Any method except `POST` | `405 Method Not Allowed`                           |

On a valid request it writes a line to the application log:

```
Received email for random@anemail.com with subject "Any subject is valid"
```

## Tech

* Java 25 (Gradle toolchain), Gradle 9 via the checked-in wrapper
* JDK built-in `com.sun.net.httpserver.HttpServer` for HTTP
* Jackson for JSON, SLF4J + Logback for logging
* JUnit 5 and RestAssured for the service test
* Deployagram JUnit 5 annotations for instrumentation

## Project layout

```
src/main/java/com/deployagram/demo/emailer/
    EmailerApplication.java   starts the HTTP server (port 8080)
    EmailHandler.java         handles POST /Emailer/email
    Email.java                the request body
src/test/java/com/deployagram/demo/emailer/
    Environment.java                   ports and URLs shared by the tests
    ReceiveEmailTest.java              service test for a valid request (instrumented)
    RejectInvalidEmailRequestTest.java service test for the 400 and 405 responses (instrumented)
```

## Run the application

```bash
./gradlew run
```

Then, from another terminal:

```bash
curl -i -X POST http://localhost:8080/Emailer/email \
  -H 'Accept: application/json' -H 'Content-Type: application/json' \
  -d '{"email":"test@deployagram.com","subject":"Hello","body":"World"}'
```

## The service test

Both test classes are black-box service tests. They start `EmailerApplication` in-process and call
it over HTTP with RestAssured. Log output is not asserted.

* `ReceiveEmailTest` POSTs the request above and asserts a `204` response with a JSON `Content-Type`.
* `RejectInvalidEmailRequestTest` asserts a `400` for a body that is not valid JSON, and a `405` for
  a request that is not a `POST`.

The test is written in a Gherkin style: each test is a single `when...` step followed by one or more
`then...` steps, and each step is a small, well-named helper method.

```java

@Test
void acceptsAnEmailRequest() {
    whenTheEmailIsPosted();

    thenTheResponseIsNoContent();
    thenTheResponseIsJson();
}
```

## How the Deployagram instrumentation works

Deployagram builds living documentation from the traffic your tests really exercise. To capture it,
the test runs a **Deployagram HTTP Proxy** between the test and the application, and a
**Collector** gathers what the proxy sees.

```
  ReceiveEmailTest ──► Deployagram HTTP Proxy ──► EmailerApplication
  (plays "EmailClient")    (Docker, port 9080)      (test JVM, port 8080)
                                   │
                                   ▼
                               Collector ──► Deployagram Cloud
                            (Docker, you start it)
```

1. The test sends its request to the **proxy** on port `9080`, not to the application directly.
2. The proxy forwards it to the application on port `8080`, and records the request and response,
   labelled with who called (`EmailClient`) and who was called (`Emailer`).
3. The proxy reports to the **Collector**, which holds the results for the test run.
4. When the build is green, you flush the Collector to Deployagram Cloud (see below).

No application code is changed, and the application does not know the proxy exists.

### In the test

Instrumentation is a handful of annotations. Both test classes carry the same ones; the first is
shown here:

```java

@Deployagram(startEnvironment = true, shareHostPorts = {Environment.APP_PORT}, proxyPort = Environment.PROXY_PORT)
@DeployagramConfig({
        @DeployagramConfigEntry(key = "proxy.namesOfSourceApps.Emailer/email", value = "EmailClient"),
        @DeployagramConfigEntry(key = "proxy.proxiedAppNames.Emailer/email", value = "Emailer"),
        @DeployagramConfigEntry(key = "proxy.proxiedApps.Emailer/email", value = Environment.APP_URL_FOR_PROXY + "/Emailer/email"),
})
class ReceiveEmailTest { ...
}
```

| Setting                                 | Meaning                                                                                                                   |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `startEnvironment = true`               | Start the Deployagram containers (using Testcontainers) before the tests run.                                             |
| `shareHostPorts = {8080}`               | Make the application's port, on the host, reachable from inside the proxy container.                                      |
| `proxyPort = 9080`                      | The host port on which the proxy listens. The test's base URI points here.                                                |
| `proxy.namesOfSourceApps.Emailer/email` | Name of the caller of this path, shown on diagrams: `EmailClient`.                                                        |
| `proxy.proxiedAppNames.Emailer/email`   | Name of the application receiving the call: `Emailer`.                                                                    |
| `proxy.proxiedApps.Emailer/email`       | Where the proxy forwards requests to. The proxy runs in Docker, so it reaches the host as `host.testcontainers.internal`. |

The only other change to each test is that its base URI is the proxy (`Environment.PROXY_BASE_URI`,
`http://localhost:9080`), so every request goes through it. That includes the `400` and `405` responses, which therefore appear in
the recorded traffic alongside the `204`.

The Deployagram environment is started once per test run and shared: the first test class to run
starts the proxy, and later classes reuse it. Because of this, every class must use the same
`proxyPort`. The ports and URLs live in one place, the test-only `Environment` class, so the test
classes do not depend on each other, and do not read any constants from the production code. The proxy
configuration is sent again before each test, which is why each class repeats the
`@DeployagramConfig` block.

### Dependencies

From `build.gradle`:

```groovy
testImplementation 'com.deployagram:deployagram-annotations-junit5:1.0.6'
testImplementation 'com.deployagram:deployagram-environment:1.0.5'
```

These come from Maven Central. The Docker images (proxy and Collector) do not; see below.

## Running the instrumented test

### Prerequisites

* **Docker** running
* A **Deployagram Cloud** account and **Docker licence** (Profile > License in Deployagram)
* The two Deployagram images pulled from the Deployagram Docker registry:
  `deployagram-collector` and `http-diagramming-proxy`
* A Docker network named `deployagram` (create it once with `docker network create deployagram`)

The Deployagram documentation (Ingredients BOM > Docker Images) explains how to log in to the
registry and pull the images.

### Configuration (kept out of git)

Create two files in the project root. Both are listed in `.gitignore`. **Never commit them.**

`.env`

```
DEPLOYAGRAM_LICENSE_KEY=<your licence key>
DEPLOYAGRAM_CLOUD_USERNAME=<your Deployagram Cloud username>
DEPLOYAGRAM_CLOUD_PASSWORD=<your Deployagram Cloud password>
DEPLOYAGRAM_CLIENT_ID=78uqtaav9ip09q2gf8qs38alke
```

`.deployagram` contains your licence file (the `-----BEGIN LICENSE FILE-----` block).

### Start the Collector

Start the Collector before running the tests, so that it captures everything. Follow the
Deployagram documentation (Collector) for the exact `docker run` command. In outline:

```bash
docker run \
  --name diagram-logger \
  --network deployagram \
  --network-alias diagram-logger \
  -e DEPLOYAGRAM_LICENSE="$(<.deployagram)" \
  --env-file .env \
  -p <YOUR_COLLECTOR_PORT>:8080 \
  325701203566.dkr.ecr.eu-west-2.amazonaws.com/deployagram-docker:deployagram-collector-multi-latest
```

### Run the tests

The `DEPLOYAGRAM_*` variables must be in the environment of the test JVM. From a terminal:

```bash
set -a; source .env; set +a; ./gradlew test
```

From IntelliJ IDEA, set *Build Tools > Gradle > Run tests using* to **IntelliJ IDEA**, and add the
variables to the JUnit run configuration (for example with the EnvFile plugin). If they are missing,
the proxy starts with a placeholder licence key, stops immediately, and the test fails with
`Connection reset`.

### Publish the results

A local run is not published automatically. To send a green run to Deployagram Cloud, mark it
complete and flush it (the container name is `Emailer`):

```bash
curl --fail-with-body -sS -X POST \
  "http://localhost:<YOUR_COLLECTOR_PORT>/run/markComplete/Emailer/<version>"

curl --fail-with-body -sS -X POST \
  "http://localhost:<YOUR_COLLECTOR_PORT>/run/flushToCloud/Emailer"
```

Only flush when the tests pass. In CI, use the version (for example the commit hash or run number)
as `<version>`.

Finally, stop the Collector when you have finished:

```bash
docker stop diagram-logger
```
