# =========================================================================
# CloudNest — Multi-Stage Docker Build
# =========================================================================
# Stage 1: Build the application using Maven
# Stage 2: Run the application using a lightweight JRE
# =========================================================================

# ── Stage 1: Build ──
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy only pom.xml first to cache dependency downloads
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR (skip tests for faster builds)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S cloudnest && adduser -S cloudnest -G cloudnest

# Create storage directories for simulated distributed nodes
RUN mkdir -p /app/storage/node1 /app/storage/node2 /app/storage/node3 \
    && chown -R cloudnest:cloudnest /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
RUN chown cloudnest:cloudnest app.jar

# Switch to non-root user
USER cloudnest

# Expose the port (Render sets PORT env var)
EXPOSE 8080

# Health check for container orchestrators
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run with production profile
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
