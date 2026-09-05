FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY backend/ backend/
COPY frontend/ frontend/
RUN chmod +x backend/mvnw \
	&& cd backend \
	&& ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system app \
	&& useradd --system --gid app --home-dir /app app
COPY --from=build --chown=app:app /workspace/backend/target/smart-ev-assistant.jar /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
