FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY configserver/pom.xml /workspace/configserver/pom.xml
COPY configserver/src /workspace/configserver/src

RUN mvn -f /workspace/configserver/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/configserver/target/*.jar /app/app.jar

EXPOSE 8888
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

