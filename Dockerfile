# TODO: include frontend build in the build process

#FROM azul/zulu-openjdk-alpine:21-latest
#COPY . /build
#WORKDIR /build
#RUN ./gradlew clean bootJar

FROM azul/zulu-openjdk-alpine:21-jre-latest
#COPY --from=0 /build/build/libs/estonian-election-vis-0.0.1.jar app.jar
COPY /build/libs/estonian-election-vis-0.0.1.jar app.jar
EXPOSE 12345
HEALTHCHECK --interval=1m --timeout=10s --start-period=30s --retries=10 CMD wget -q --spider http://localhost:12345/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production", "/app.jar"]
