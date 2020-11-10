FROM nikolaik/python-nodejs:python3.8-nodejs14-slim

COPY . /fetcher-pipeline
WORKDIR /fetcher-pipeline

RUN pip3.8 install -r requirements.txt

RUN npm install pm2 -g

# Install java
ENV JAVA_HOME /usr/local/jdk-11.0.2
ENV PATH $JAVA_HOME/bin:$PATH

RUN apt update && apt install curl -y

RUN curl -O https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz && \
    tar zxvf openjdk-11.0.2_linux-x64_bin.tar.gz && mv jdk-11* /usr/local/

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
