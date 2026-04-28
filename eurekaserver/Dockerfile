FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY eurekaserver/pom.xml /workspace/eurekaserver/pom.xml
COPY eurekaserver/src /workspace/eurekaserver/src

RUN mvn -f /workspace/eurekaserver/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/eurekaserver/target/*.jar /app/app.jar

EXPOSE 8761
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

