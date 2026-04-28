FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY paymentservice/pom.xml /workspace/paymentservice/pom.xml
COPY paymentservice/src /workspace/paymentservice/src

RUN mvn -f /workspace/paymentservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/paymentservice/target/*.jar /app/app.jar

EXPOSE 8084
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

