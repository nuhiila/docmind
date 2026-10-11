# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home spring \
 && mkdir -p /data/uploads /tmp/spring-ai-onnx-model \
 && chown -R spring /data/uploads /tmp/spring-ai-onnx-model

COPY --from=build /workspace/target/*.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]