FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY catalogservice/pom.xml /workspace/catalogservice/pom.xml
COPY catalogservice/src /workspace/catalogservice/src

RUN mvn -f /workspace/catalogservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/catalogservice/target/*.jar /app/app.jar

EXPOSE 8082
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

