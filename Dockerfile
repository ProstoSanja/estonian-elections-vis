FROM azul/zulu-openjdk-alpine:17-jre
EXPOSE 80
COPY build/libs/RK2023-0.0.1.jar app.jar
ENTRYPOINT ["java","-jar", "/app.jar"]