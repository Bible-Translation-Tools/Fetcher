FROM gradle:8.5-jdk17 AS builder
COPY . /fetcher-web
WORKDIR /fetcher-web
RUN gradle build --info --stacktrace || (ls -R build/reports/problems && exit 1)

FROM eclipse-temurin:17-jre-jammy

RUN apt update && apt install git -y

RUN mkdir /app

COPY --from=builder /fetcher-web/build/libs/bible-translation-tools_fetcher.jar /app/fetcher.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "fetcher.jar"]
