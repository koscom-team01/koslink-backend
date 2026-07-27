# Build Stage
FROM gradle:jdk17 AS builder
WORKDIR /app

# Gradle 설정 및 소스 코드 복사
COPY build.gradle settings.gradle ./
COPY src src

# 이미 내장된 Gradle을 사용하여 실행 가능한 JAR 빌드 (외부 gradle-wrapper 다운로드 방지)
RUN gradle bootJar -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# 시스템 시간대 설정 (apt-get 네트워크 타임아웃 지연 원인 제거)
ENV TZ=Asia/Seoul

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
