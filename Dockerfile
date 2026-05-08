# Runtime-only Dockerfile
# Pre-requisite: build the JAR locally first with:
#   mvn clean package -DskipTests
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the pre-built JAR from local target/
COPY target/price-tracker-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
