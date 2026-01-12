# Stage 1: Build
FROM maven:3.9.5-openjdk-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app

# Install Tesseract + all language packs
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-all libtesseract-dev libleptonica-dev pkg-config && \
    rm -rf /var/lib/apt/lists/*

# Set TESSDATA_PREFIX to parent of tessdata folder
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr

# Copy built jar
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
