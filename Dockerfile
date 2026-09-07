# Multi-stage production image for arena-set-sharer
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache su-exec \
  && addgroup -S app && adduser -S app -G app \
  && mkdir -p /covers && chown app:app /covers

COPY --from=build /workspace/build/libs/ /tmp/libs/
RUN JAR="$(ls /tmp/libs/*.jar | grep -v plain | head -1)" \
  && cp "$JAR" /app/app.jar \
  && rm -rf /tmp/libs \
  && chown -R app:app /app

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
