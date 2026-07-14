# Build the executable JAR locally before building this image:
# mvn clean package -DskipTests
FROM eclipse-temurin:8-jre-jammy

WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

# Keep the application and its logs out of the root filesystem/user.
RUN groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --create-home --home-dir /app --shell /usr/sbin/nologin spring \
    && mkdir -p /app/logs /app/config \
    && chown -R spring:spring /app

ARG JAR_FILE=target/hero-middleware-1.0.0.jar
COPY --chown=spring:spring ${JAR_FILE} /app/app.jar

USER spring

# application-prod.yml listens on 8087 by default.
EXPOSE 8087

# Optional external configuration can be mounted to /app/config.
VOLUME ["/app/logs", "/app/config"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=$TZ -jar /app/app.jar"]
