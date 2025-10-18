FROM azul/zulu-openjdk-alpine:21-latest
WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean bootJar --no-daemon

FROM azul/zulu-openjdk-alpine:21-jre-latest
COPY --from=0 /build/build/libs/estonian-election-vis-0.0.1.jar app.jar
EXPOSE 12345
HEALTHCHECK --interval=1m --timeout=10s --start-period=30s --retries=10 CMD wget -q --spider http://localhost:12345/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production", "/app.jar"]