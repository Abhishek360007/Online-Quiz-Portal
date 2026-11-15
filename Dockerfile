# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Download dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests \
    && cp target/online-quiz-portal-*.jar app.jar

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S portal && adduser -S portal -G portal

# Unpack the fat jar into a plain classpath (required for the AOT cache)
COPY --from=build /workspace/app.jar /tmp/app.jar
RUN java -Djarmode=tools -jar /tmp/app.jar extract --destination /app/application \
    && rm /tmp/app.jar

# Training run: start the Spring context once, without a database, and record a
# class-data-sharing (CDS) archive of the classes it loads. Startup then maps them
# instead of parsing them again. If the archive ever doesn't match, the JVM just
# ignores it (-Xshare:auto) and starts normally.
RUN java -XX:ArchiveClassesAtExit=/app/app.jsa \
        -Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
        -Dspring.context.exit=onRefresh \
        -Dspring.jpa.hibernate.ddl-auto=none \
        -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
        -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
        -jar /app/application/app.jar \
    && chown -R portal:portal /app

USER portal

# Tuned for small containers (e.g. Render free tier: 512 MB, shared CPU)
ENV JAVA_OPTS="-XX:SharedArchiveFile=/app/app.jsa -Xshare:auto -Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError" \
    SPRING_THYMELEAF_CACHE=true \
    PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=3 \
    CMD wget -qO- http://localhost:${PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application/app.jar"]
