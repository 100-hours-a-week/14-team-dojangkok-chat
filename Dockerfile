FROM eclipse-temurin:21-jre-noble

WORKDIR /app

COPY chatting-be-jar.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
