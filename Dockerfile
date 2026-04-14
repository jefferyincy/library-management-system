# -----------------------------------------------------------------------------
# Multi-stage build: (1) compile with Maven, (2) run with only the JRE — small image.
# Build:  docker build -t library-web .
# Run:    docker run --rm -p 8080:8080 -v library-data:/data library-web
#          (-v keeps books.dat / users.dat / issues.dat in a Docker volume)
# -----------------------------------------------------------------------------

# --- Stage 1: build the Spring Boot fat JAR ---
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /src

# Layer cache: only re-runs mvn when pom changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Stage 2: runtime (no Maven/JDK in final image) ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user (good practice on shared hosts)
RUN addgroup -S spring && adduser -S spring -G spring

# JAR name comes from <finalName>app</finalName> in pom.xml → target/app.jar
COPY --from=build /src/target/app.jar app.jar

# Writable dir for serialized .dat files — mount a volume here in production
RUN mkdir -p /data && chown spring:spring /data

USER spring:spring

# App reads these (see application.properties)
ENV LIBRARY_DATA_DIR=/data
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Optional: tune heap for small containers (e.g. 512m free tier)
# ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
