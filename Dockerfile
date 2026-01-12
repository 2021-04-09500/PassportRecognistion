# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM debian:bookworm-slim

# Install Tesseract 4 and English language data
RUN apt-get update && \
    apt-get install -y tesseract-ocr libtesseract-dev tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

# Set the Tesseract data path
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
