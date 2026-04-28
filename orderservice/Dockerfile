FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY orderservice/pom.xml /workspace/orderservice/pom.xml
COPY orderservice/src /workspace/orderservice/src

RUN mvn -f /workspace/orderservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/orderservice/target/*.jar /app/app.jar

EXPOSE 8083
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

