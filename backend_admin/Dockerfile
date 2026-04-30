FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/logs /app/storage \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
