# ── Builder stage ───────────────────────────────────────────────────────────────
FROM azul/zulu-openjdk:17 AS builder

WORKDIR /app

# Copy Maven files first for better caching
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build the JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM azul/zulu-openjdk:17-jre

WORKDIR /app

# Install Tesseract + English traineddata
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        libtesseract-dev \
        tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Set TESSDATA_PREFIX (parent of tessdata folder)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]