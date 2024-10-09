FROM nikolaik/python-nodejs:python3.12-nodejs22-slim

RUN npm install pm2 -g
RUN pm2 install pm2-logrotate
RUN pm2 flush
RUN pm2 set pm2-logrotate:compress true && \
    pm2 set pm2-logrotate:max_size 10M && \
    pm2 set pm2-logrotate:retain 10 && \
    pm2 set pm2-logrotate:workerInterval 300

# Install java
ENV JAVA_HOME /usr/local/jdk-11.0.2
ENV PATH $JAVA_HOME/bin:$PATH

RUN apt update && apt install curl ffmpeg git -y

RUN curl -O https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz && \
    tar zxvf openjdk-11.0.2_linux-x64_bin.tar.gz && mv jdk-11* /usr/local/

WORKDIR /fetcher-pipeline
COPY requirements.txt /fetcher-pipeline
RUN pip3 install -r requirements.txt
COPY . /fetcher-pipeline


COPY entrypoint.sh /entrypoint.sh
COPY gl_repo_urls.txt /repourls/gl_repo_urls.txt
ARG debugpy
ENV DEBUGPY=${debugpy}
ENTRYPOINT ["/entrypoint.sh"]
