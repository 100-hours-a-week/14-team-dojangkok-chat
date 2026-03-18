FROM eclipse-temurin:21-jre-noble

WORKDIR /app

COPY chatting-be-jar.jar app.jar
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
