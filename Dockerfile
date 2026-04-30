FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/basic-lottery-1.0.0-jar-with-dependencies.jar /app/app.jar
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]
