FROM maven:3.5-jdk-8-alpine as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
CMD ["/script.sh"]

RUN mvn package -DskipTests
