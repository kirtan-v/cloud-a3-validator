FROM openjdk:17
WORKDIR /app
COPY target/validator-0.0.1-SNAPSHOT.jar app.jar
VOLUME ["/app/data"]
EXPOSE 6000
ENTRYPOINT ["java", "-jar", "app.jar"]
