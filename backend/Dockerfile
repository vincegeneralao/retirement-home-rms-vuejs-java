# ---- Build stage: compile the fat jar with Maven on JDK 25 ----
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Cache dependencies first (re-resolved only when the POM changes).
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage: minimal JRE ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --create-home --shell /usr/sbin/nologin rms
USER rms
COPY --from=builder /app/target/rms-backend.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
