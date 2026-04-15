FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY redrolls.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]