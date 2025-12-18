#BUILD STAGE
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

#RUNTIME STAGE
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
