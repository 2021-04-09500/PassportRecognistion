# Stage 1: build
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Tesseract + all language packs
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-all libtesseract-dev libleptonica-dev pkg-config && \
    rm -rf /var/lib/apt/lists/*

# Set TESSDATA_PREFIX so Tess4J knows where to find traineddata
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata/

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-jar","app.jar"]
