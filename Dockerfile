# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with JDK (Temurin)
FROM eclipse-temurin:17-jdk AS runtime

# Install Tesseract 4 + English traineddata
RUN apt-get update && \
    apt-get install -y tesseract-ocr libtesseract-dev tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

# Tesseract path
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
