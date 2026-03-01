FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

# 배포 환경에서 항상 prod 프로파일 사용 (Secure 쿠키, CORS 환경변수 필수화 등 보안 설정 보장)
ENV SPRING_PROFILES_ACTIVE=prod

# [필수] 컨테이너 실행 시 반드시 CORS_ALLOWED_ORIGINS 환경변수를 주입해야 합니다.
# 미설정 시 애플리케이션이 시작되지 않습니다 (application-prod.properties 참고)
# 예시: docker run -e CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:5173 ...

EXPOSE 8080

# 0.5GB 컨테이너 환경 JVM 튜닝
# -XX:+UseSerialGC: GC 스레드 오버헤드 최소화 (소규모 컨테이너에 적합)
# -XX:MaxRAMPercentage=60: 컨테이너 메모리의 60%(~300MB)를 힙으로 사용
# -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m: Metaspace 크기 제한
# -Xss256k: 스레드 스택 기본값(512k) 절반으로 절약
ENTRYPOINT ["java", \
  "-XX:+UseSerialGC", \
  "-XX:MaxRAMPercentage=60.0", \
  "-XX:MetaspaceSize=64m", \
  "-XX:MaxMetaspaceSize=128m", \
  "-Xss256k", \
  "-jar", "app.jar"]
