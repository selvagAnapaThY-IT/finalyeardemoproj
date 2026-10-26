FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN mvn -B verify
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/fintaxpro-api-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
