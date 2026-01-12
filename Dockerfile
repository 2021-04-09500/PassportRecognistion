# ── Builder stage ───────────────────────────────────────────────────────────────
FROM azul/zulu-openjdk:17 AS builder

WORKDIR /app

# Copy Maven wrapper and config first (for caching)
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Make mvnw executable – this fixes the Permission denied (exit 126)
RUN chmod +x ./mvnw

# Now download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM azul/zulu-openjdk:17-jre

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        libtesseract-dev \
        tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]