# =================================================================
# Stage 1: Build Stage
# Gradle을 사용하여 Spring Boot 애플리케이션을 빌드하는 단계
# =================================================================
# 버전을 gradle:8.5.0-jdk25-jammy로 변경
# jdk25와 gradle 포함
FROM gradle:8.5.0-jdk25-jammy AS build

# 작업 디렉토리 설정
WORKDIR /app

# build.gradle 파일과 소스 코드를 컨테이너 안으로 복사
COPY . .

# Gradle wrapper를 사용하여 애플리케이션을 빌드 (테스트는 생략하여 빌드 속도 향상)
# 이 명령어가 실행되면 build/libs/backend-0.0.1-SNAPSHOT.jar 파일이 생성됩니다.
RUN ./gradlew build -x test


# =================================================================
# Stage 2: Run Stage
# 빌드된 애플리케이션을 실행하는 최종 이미지를 만드는 단계
# =================================================================
# 버전을 eclipse-temurin:25-jre-jammy로 변경
FROM eclipse-temurin:25-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# build 스테이지에서 생성된 JAR 파일을 현재 스테이지로 복사
COPY --from=build /app/build/libs/backend-0.0.1-SNAPSHOT.jar ./app.jar

# 컨테이너 외부에서 8080 포트로 접근할 수 있도록 포트 개방
EXPOSE 8080

# 컨테이너가 시작될 때 실행할 명령어
# java -jar app.jar 명령으로 Spring Boot 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]