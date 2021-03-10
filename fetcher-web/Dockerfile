FROM gradle:6.5-jdk11 AS builder
COPY . /fetcher-web
WORKDIR /fetcher-web
RUN gradle check build

FROM openjdk:11-jre-slim

RUN mkdir /app

COPY --from=builder /fetcher-web/build/libs/bible-translation-tools_fetcher.jar /app/fetcher.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "fetcher.jar"]
