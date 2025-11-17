# ------------------------------------
# Stage 1: Build Stage (Builder)
# ------------------------------------
# Use a robust, common JDK base image for compiling and packaging.
FROM eclipse-temurin:21-jdk AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper files and pom.xml first to leverage Docker's layer caching.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# This command runs if the pom.xml changes. It downloads dependencies.
#RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src src

# Build the application, skipping tests for faster image creation.
# The resulting JAR will be in target/
RUN ./mvnw package -DskipTests

# ------------------------------------
# Stage 2: Final Image (Runner)
# ------------------------------------
# Use a minimal JRE base image for the final runtime. 
# This is smaller and more secure than using the full JDK from the build stage.
FROM eclipse-temurin:21-jre

# Set the deployment environment port
EXPOSE 8080

# Set a variable for the JAR file name based on your Maven build output
ARG JAR_FILE=target/*.jar

# Copy the built JAR file from the 'builder' stage into the final image
COPY --from=builder /app/${JAR_FILE} app.jar

# Define the entrypoint to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app.jar"]

# Note: For connecting to external Kafka/DB, you will configure 
# the bootstrap servers via Kubernetes environment variables later.