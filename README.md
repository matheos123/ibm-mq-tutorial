## 1. Get ready

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


## 2. Start IBM MQ

From the project root:

```sh
docker compose up -d --wait --wait-timeout 240
docker compose ps
```
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

