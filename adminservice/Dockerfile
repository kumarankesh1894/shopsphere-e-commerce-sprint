FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY adminservice/pom.xml /workspace/adminservice/pom.xml
COPY adminservice/src /workspace/adminservice/src

RUN mvn -f /workspace/adminservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/adminservice/target/*.jar /app/app.jar

EXPOSE 8085
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

