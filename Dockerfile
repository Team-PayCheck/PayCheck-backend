FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

# 배포 환경에서 항상 prod 프로파일 사용 (Secure 쿠키, CORS 환경변수 필수화 등 보안 설정 보장)
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
