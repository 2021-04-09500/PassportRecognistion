# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Tesseract and ALL languages
RUN apt-get update && \
    apt-get install -y \
        tesseract-ocr \
        tesseract-ocr-all \
        libtesseract-dev \
        libleptonica-dev && \
    rm -rf /var/lib/apt/lists/*

# 🔑 THIS IS THE FIX
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/tessdata

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-jar","app.jar"]
