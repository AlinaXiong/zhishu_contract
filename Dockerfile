FROM maven:3.9-eclipse-temurin-8 AS builder

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode dependency:go-offline

COPY src ./src
RUN mvn --batch-mode -Dmaven.test.skip=true package \
    && JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*-original.jar' -print -quit)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /tmp/app.jar

FROM eclipse-temurin:8-jre-jammy

WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

# Keep the application and its logs out of the root filesystem/user.
RUN groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --create-home --home-dir /app --shell /usr/sbin/nologin spring \
    && mkdir -p /app/logs /app/config \
    && chown -R spring:spring /app

COPY --from=builder --chown=spring:spring /tmp/app.jar /app/app.jar

USER spring

# application-prod.yml listens on 8087 by default.
EXPOSE 8087

# Optional external configuration can be mounted to /app/config.
VOLUME ["/app/logs", "/app/config"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=$TZ -jar /app/app.jar"]
