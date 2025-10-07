# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Сначала pom, прогреем зависимости
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Теперь исходники и сборка fat-jar (shade)
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -m -u 10001 appuser

RUN mkdir -p /data && chown -R appuser:appuser /data

USER appuser

ENV PORT=8080 \
    BASE_URL=http://localhost \
    DB_URL=jdbc:postgresql://db:5432/filesvc \
    DB_USER=filesvc \
    DB_PASSWORD=filesvc \
    STORAGE_DIR=/data \
    MAX_UPLOAD_MB=256 \
    RETENTION_DAYS=30

# Копируем толстый jar
COPY --from=build /app/target/server.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
