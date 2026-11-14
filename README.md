# IBM MQ + Spring Boot messaging demo

Learn how two Spring Boot applications communicate asynchronously through a real IBM MQ queue.

```text
HTTP request -> producer-service -> IBM MQ: Sendly -> consumer-service -> log
                 localhost:8081      localhost:1414
```

The producer accepts plain text at `POST /api/messages` and sends a JMS text message using `JmsTemplate`. IBM MQ stores the message until the consumer's `@JmsListener` receives it and logs its contents. The HTTP response confirms that MQ accepted the message; it does not wait for the consumer to process it.

Only IBM MQ runs in Docker. Both Java applications run locally, and the consumer has no HTTP server. There is no database or HTTP authentication.

## 1. Get ready

Download or clone the complete project, then open a terminal in the folder containing `compose.yaml` and the root `pom.xml`. Keep the entire folder when sharing it, including the hidden `.mvn` directories and the `docker` password files.

You need:

- **JDK 25**, with `JAVA_HOME` pointing to its installation directory.
- **Docker Desktop** running with Linux containers and Docker Compose v2. On Linux, Docker Engine with Compose v2 also works.
- Internet access for the first run to download Maven, Java dependencies, and the MQ image.
- Available ports **8080**, **1414**, and **9443**.

Maven is downloaded automatically by the included Maven Wrapper. You do not need to install Maven separately.

Check the tools in **Windows PowerShell**:

```powershell
java -version
.\mvnw.cmd -version
docker version
docker compose version
```

On **macOS, Linux, or Git Bash**, use `./mvnw` instead of `.\mvnw.cmd`:

```sh
java -version
./mvnw -version
docker version
docker compose version
```

Both Java and the Java version reported by Maven must be **25**. Docker must show a running server as well as a client. If Linux/macOS reports permission denied for the wrapper, run `chmod +x mvnw` first.

## 2. Start IBM MQ

From the project root:

```sh
docker compose up -d --wait --wait-timeout 240
docker compose ps
```

The first startup can take several minutes. Continue when the `mq` service is **healthy**.

Compose starts IBM MQ Advanced for Developers and creates queue manager **QM1**. Its developer defaults provide the **DEV.APP.SVRCONN** connection channel and **DEV.QUEUE.1** queue. The image also creates other development queues; this demo uses only `DEV.QUEUE.1`.

The Compose setting `LICENSE=accept` accepts IBM's license for the developer image. The broker and web console ports are bound to localhost.

## 3. Run the two applications

Open two terminals in the project root. Leave each application running in its terminal.

**Terminal 1 — consumer:**

```powershell
.\mvnw.cmd -q -pl consumer-service spring-boot:run
```

**Terminal 2 — producer:**

```powershell
.\mvnw.cmd -q -pl producer-service spring-boot:run
```

On macOS/Linux/Git Bash, the equivalent commands are:

```sh
# Terminal 1
./mvnw -pl consumer-service spring-boot:run
```

```sh
# Terminal 2
./mvnw -pl producer-service spring-boot:run
```

Wait for each application's `Started ...Application` log. The first run downloads dependencies and compiles the code automatically.

Each service also includes its own wrapper. If your terminal is already inside `producer-service` or `consumer-service`, run `.\mvnw.cmd spring-boot:run` on PowerShell or `./mvnw spring-boot:run` on Bash, without `-pl`.

## 4. Send a message

Open a third terminal in the project root.

### Option A: Send a Structured JSON Message (Recommended)

```powershell
curl.exe -i -X POST http://localhost:8081/api/messages `
  -H "Content-Type: application/json" `
  -d '{\"sender\":\"Alice\",\"content\":\"Deployment order #42\",\"priority\":\"HIGH\"}'
```

On Linux/macOS/Git Bash:
```sh
curl -i -X POST http://localhost:8081/api/messages \
  -H "Content-Type: application/json" \
  -d '{"sender":"Alice","content":"Deployment order #42","priority":"HIGH"}'
```

**Producer logs:**
```text
[Producer] Sent: [id=d3e89a12] [from=Alice] [priority=HIGH] content='Deployment order #42'
```

**Consumer logs:**
```text
[Consumer] Received JSON: [id=d3e89a12] [from=Alice] [priority=HIGH] content='Deployment order #42'
```

### Option B: Send Plain Text (Backwards Compatible)

```powershell
curl.exe -i -X POST http://localhost:8081/api/messages -H "Content-Type: text/plain" -d "Hello IBM MQ!"
```

## 5. Interactive Swagger / OpenAPI UI

Both services feature embedded Swagger UIs:

| Service | Swagger UI URL | Features |
| --- | --- | --- |
| **Producer Service** (Port 8081) | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | Interactive UI to publish JSON and plain text messages to `Sendly` |
| **Consumer Service** (Port 8082) | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | Interactive UI to view audit history, statistics, DLQ messages, and trigger DLQ replay |

*(For remote teammates on your network, replace `localhost` with your IP `192.168.0.105`)*

## 6. Consumer Audit & Dead Letter Queue (DLQ) APIs

The `consumer-service` runs a web server on port **8082** with full audit tracking and poison-pill recovery:

| Endpoint | Method | Description |
| --- | --- | --- |
| `http://localhost:8082/api/messages` | `GET` | List all processed messages (`?status=PROCESSED` or `?status=DEAD_LETTER`) |
| `http://localhost:8082/api/messages/stats` | `GET` | Message statistics (total, processed, dead-lettered) |
| `http://localhost:8082/api/messages/dlq` | `GET` | List only dead-lettered messages with error reasons |
| `http://localhost:8082/api/messages/dlq/replay` | `POST` | Replay all messages from `Sendly.DLQ` back to `Sendly` queue |

### Simulating a Poison Pill & DLQ

Send a message containing `"FAIL"` to trigger retry & DLQ routing:

```powershell
curl.exe -i -X POST http://localhost:8081/api/messages `
  -H "Content-Type: application/json" `
  -d '{\"sender\":\"Alice\",\"content\":\"FAIL: database down\",\"priority\":\"HIGH\"}'
```

Watch the consumer logs:
1. Message fails business validation.
2. Consumer retries 3 times with backoff.
3. Consumer exhausts retries and automatically routes the message to **`Sendly.DLQ`**.
4. Check `GET http://localhost:8082/api/messages/stats` to verify DLQ count.
5. Replay it when fixed with `POST http://localhost:8082/api/messages/dlq/replay`.

## 7. See the queue in the IBM MQ web console

Open **[https://localhost:9443/ibmmq/console](https://localhost:9443/ibmmq/console)**.

The local console uses a self-signed HTTPS certificate. If your browser warns about it, use its Advanced option to continue to localhost. The console may take a little longer to start after MQ becomes healthy; retry the page if needed.

Sign in with:

| Setting | Value |
| --- | --- |
| Username | `admin` |
| Password | `passw0rd` |

To watch a message wait in the queue:

1. Stop the consumer in terminal 1 with **Ctrl+C**. Keep MQ and the producer running.
2. Send another message using the curl command above. The producer still returns HTTP 200 because MQ can accept messages while the consumer is offline.
3. In the console, open queue manager **QM1** and find **Sendly** in its queue list.
4. Refresh the queue list. The current depth increases by one for each message sent while the consumer is stopped. Open the queue and use its message browsing action to inspect the waiting messages.
5. Restart the consumer using its command from step 3. Watch it log the waiting messages.
6. Refresh the console. The queue depth should return to **0**.

With the consumer running, messages may be consumed too quickly to appear in the console. A queue delivers each message to one consumer; this example does not implement application-level deduplication or exactly-once processing.

## Configuration at a glance

| Component | Configuration |
| --- | --- |
| Producer endpoint | `POST http://localhost:8081/api/messages`, body type `application/json` or `text/plain` |
| MQ connection | `localhost(1414)` |
| Queue manager | `QM1` |
| Application channel | `DEV.APP.SVRCONN` |
| Queue used by both services | `Sendly` |
| MQ application login | `app` / `passw0rd` |
| Web console login | `admin` / `passw0rd` |

Both services' connection settings are in their respective `src/main/resources/application.yaml` files. Compose mounts `docker/app-password.txt` and `docker/admin-password.txt` as MQ password secrets. These fixed credentials are for this local learning demo.

Pinned versions: **Java 25**, **Spring Boot 4.1.2**, **IBM MQ JMS Spring Boot starter 4.1.2**, **Maven 3.9.16**, and **IBM MQ developer image 9.4.0.5-r2**. The IBM starter includes the Jakarta JMS client and Spring JMS.

## Stop or reset

Stop both Java applications with **Ctrl+C**, then stop MQ:

```sh
docker compose down
```

The named Docker volume retains MQ data for the next startup. To deliberately delete all demo queue data and start fresh:

```sh
docker compose down -v
```

After changing `compose.yaml`, rerun `docker compose up -d --wait --wait-timeout 240` to apply the changes.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| Java command missing or compilation fails | Install JDK 25, check `JAVA_HOME` and PATH, then reopen the terminal. Confirm the Java version in the wrapper's `-version` output. |
| Wrapper cannot download Maven or dependencies | Check internet access and any proxy settings. The first run needs downloads. |
| `./mvnw` gives permission denied | On Linux/macOS, run `chmod +x mvnw` in the folder containing that script. |
| Docker cannot connect to its server | Start Docker Desktop and wait for it to finish starting. On Windows, select Linux containers. |
| MQ startup fails | Run `docker compose logs mq` and check that ports 1414 and 9443 are free. |
| Producer cannot start | Check whether another process is using port 8080. |
| HTTP request cannot connect | Wait for the producer's startup log and confirm it is still running. |
| HTTP 503 or MQ connection errors | Confirm MQ is healthy with `docker compose ps`; check that both YAML files use the configured channel and application credentials. |
| Console page does not open | Check the 9443 mapping with `docker compose ps`, wait a little longer, and inspect `docker compose logs mq`. |
| No messages visible in the console | Stop the consumer before sending, then refresh the queue view. |

The endpoint returns **400** for an empty or blank message and **503** if sending to MQ fails.

## Optional: build executable JARs

From the root, build both services:

```powershell
.\mvnw.cmd clean package
```

Use `./mvnw clean package` on Bash. Then, in separate terminals, run:

```sh
java -jar consumer-service/target/consumer-service-1.0.0.jar
```

```sh
java -jar producer-service/target/producer-service-1.0.0.jar
```

## Project layout

```text
compose.yaml           IBM MQ broker and web console
pom.xml                Shared dependency versions and Maven modules
mvnw / mvnw.cmd        Root Maven Wrapper launchers
.mvn/wrapper/          Maven Wrapper configuration
docker/                Local demo app and admin password files
producer-service/      HTTP controller, JMS sender, YAML, and wrapper
consumer-service/      JMS listener, YAML, and wrapper
```

## Verification status

Source and configuration have been reviewed. Compilation, container startup, console login, and end-to-end delivery have not yet been verified in the authoring environment. Steps 2–5 describe the expected results so you can verify the demo on your machine.

## References

- [IBM MQ Spring starter and examples](https://github.com/ibm-messaging/mq-jms-spring)
- [IBM starter version alignment](https://github.com/ibm-messaging/mq-jms-spring/blob/v4.1.2/boot4.properties)
- [IBM developer container defaults and console](https://github.com/ibm-messaging/mq-container/blob/master/docs/developer-config.md)
- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html)
