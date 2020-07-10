FROM openjdk:11-jre-slim

ENV APPLICATION_USER ktor
RUN useradd -ms /bin/bash $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/libs/bible-translation-tools_fetcher.jar /app/my-application.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "my-application.jar"]