# 1. Gradle 빌드용 베이스 이미지 (JDK 21)
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN gradle clean build -x test --no-daemon
# RUN gradle clean build --no-daemon

# 2. 실행용 이미지 (JDK 21)
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]