# syntax=docker/dockerfile:1

# 1. Собираем фронтенд вместе со сгенерированным TypeScript SDK.
FROM node:24-alpine AS frontend
WORKDIR /workspace
COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN npm --prefix frontend ci
COPY api/openapi/openapi.yaml ./api/openapi/openapi.yaml
COPY frontend ./frontend
RUN npm --prefix frontend run sdk:gen && npm --prefix frontend run build

# 2. Собираем Spring Boot и кладём статику фронтенда внутрь jar.
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /workspace/frontend/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# 3. Минимальный runtime-образ. Приложение слушает порт из переменной PORT.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --uid 10001 calendar
COPY --from=backend /workspace/backend/target/*.jar /app/app.jar
USER calendar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
