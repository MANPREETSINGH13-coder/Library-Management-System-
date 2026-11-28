FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace/backend
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/target/library-management-1.0.0.jar /app/library-management.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/library-management.jar"]