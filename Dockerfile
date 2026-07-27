# Build Stage
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Gradle 캐시 활용을 위한 종속성 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Gradle 실행 권한 부여 및 의존성 다운로드
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon || true

# 전체 소스 코드 복사 및 실행 가능한 JAR 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# 시스템 시간대 및 보완 설정
RUN apt-get update && apt-get install -y tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/localtime && \
    rm -rf /var/lib/apt/lists/*

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
