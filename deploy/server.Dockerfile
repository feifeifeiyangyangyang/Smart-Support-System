FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace/server
COPY server/pom.xml .
COPY server/src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/server/target/smart-customer-service-0.1.0.jar app.jar
RUN mkdir -p /app/data/documents
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
