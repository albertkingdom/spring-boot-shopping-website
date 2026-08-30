FROM eclipse-temurin:8

WORKDIR /workspace/app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN ./mvnw install -DskipTests

EXPOSE 8080
ENTRYPOINT ["java","-jar","target/shopping-website-0.0.1-SNAPSHOT.jar"]
