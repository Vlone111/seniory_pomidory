#BUILD STAGE
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

# Копируем файлы для кэширования зависимостей
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Загружаем зависимости с увеличенными таймаутами
RUN gradle dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000 || true

# Копируем исходники и собираем
COPY . .
RUN gradle clean bootJar --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000

#RUNTIME STAGE
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
