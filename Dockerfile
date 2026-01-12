# Stage 1: Build
FROM maven:3.9.5-openjdk-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-slim
WORKDIR /app

# Install Tesseract + all languages + dependencies
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-all libtesseract-dev libleptonica-dev pkg-config && \
    rm -rf /var/lib/apt/lists/*

# Tess4J expects parent of tessdata
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr

# Copy jar from build stage
COPY --from=build /build/target/*.jar app.jar

# Expose port 8080 (Render will override via $PORT if needed)
EXPOSE 8080

# Run the Spring Boot app
CMD ["java", "-jar", "app.jar"]
