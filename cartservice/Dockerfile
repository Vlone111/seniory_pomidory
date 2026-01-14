# Используем официальный образ Gradle
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Копируем все файлы проекта
COPY . .

# Собираем проект (используем gradle из PATH)
RUN gradle clean bootJar -x test --no-daemon

# Финальный образ
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
