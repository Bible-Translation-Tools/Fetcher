# Build stage: Gradle version matches gradle/wrapper/gradle-wrapper.properties
FROM gradle:8.10.2-jdk21 AS builder
COPY . /fetcher-web
WORKDIR /fetcher-web
RUN gradle build --info --stacktrace || (ls -R build/reports/problems && exit 1)

# Runtime stage: JRE only, Ubuntu 24.04 (noble)
FROM eclipse-temurin:21-jre-noble

# UID/GID 105:106 match the "ftp" user in the ftp service and the pipeline image,
# so all services can read and write the shared volumes.
ARG APP_UID=105
ARG APP_GID=106

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" app \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" --create-home --shell /usr/sbin/nologin app \
    && mkdir -p /app /content /repos \
    && chown app:app /content /repos

COPY --from=builder /fetcher-web/build/libs/bible-translation-tools_fetcher.jar /app/fetcher.jar
WORKDIR /app

USER app
EXPOSE 8080
CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "fetcher.jar"]
