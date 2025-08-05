# 1. Gradle 빌드용 베이스 이미지 (JDK 21)
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN gradle clean build -x test --no-daemon
# RUN gradle clean build --no-daemon

# 2. 실행용 이미지 (JDK 21) + 크롤링용 브라우저
FROM openjdk:21-jdk-slim

# 크롤링을 위한 Chrome 브라우저 설치
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    unzip \
    curl \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Chrome이 root로 실행되도록 설정
ENV CHROME_BIN=/usr/bin/google-chrome-stable
ENV CHROME_OPTS="--no-sandbox --disable-dev-shm-usage --headless --disable-gpu --remote-debugging-port=9222"

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]