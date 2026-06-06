ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY settings.xml /usr/share/maven/conf/settings.xml

COPY pom.xml .

RUN mvn dependency:resolve dependency:resolve-plugins -Dmaven.test.skip=true

COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true

FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/swap-market.jar app.jar

EXPOSE 9024

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
