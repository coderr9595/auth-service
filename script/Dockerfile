# Use Eclipse Temurin OpenJDK 21 Alpine image as base
FROM eclipse-temurin:21-jdk-alpine
  
  # Set working directory inside container
WORKDIR /app
  
  # Copy the built jar from the host (project root) into the container
COPY ../target/auth-service-0.0.1-SNAPSHOT.jar app.jar
  
  # Expose port 8080 for the Spring Boot application
EXPOSE 8080
  
  # Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
