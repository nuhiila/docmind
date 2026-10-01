# DocMind

AI-powered document assistant built with Java and Spring Boot.
Upload documents, then ask questions and get answers with sources (RAG).

## Tech stack
- Java 21, Spring Boot
- PostgreSQL + pgvector
- Docker Compose

## Run locally
1. Start the database: `docker compose up -d`
2. Start the app: `.\mvnw spring-boot:run`
3. Check: http://localhost:8080/api/health

## Status
In progress.
