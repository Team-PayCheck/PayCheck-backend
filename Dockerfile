FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

# 배포 환경에서 항상 prod 프로파일 사용 (Secure 쿠키, CORS 환경변수 필수화 등 보안 설정 보장)
ENV SPRING_PROFILES_ACTIVE=prod

# [필수] 컨테이너 실행 시 반드시 CORS_ALLOWED_ORIGINS 환경변수를 주입해야 합니다.
# 미설정 시 애플리케이션이 시작되지 않습니다 (application-prod.properties 참고)
# 예시: docker run -e CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:5173 ...

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
