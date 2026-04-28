FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY authservice/pom.xml /workspace/authservice/pom.xml
COPY authservice/src /workspace/authservice/src

RUN mvn -f /workspace/authservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/authservice/target/*.jar /app/app.jar

EXPOSE 8081
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

