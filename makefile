.PHONY: build run stop

build:
	export FETCHER_IMAGE_TAG="local" \
	&& cd fetcher-web \
	&& ./gradlew check build \
	&& docker build -t bibletranslationtools/fetcher:$${FETCHER_IMAGE_TAG} . \
	&& cd ../fetcher-pipeline \
	&& docker build -t bibletranslationtools/fetcher-pipeline:$${FETCHER_IMAGE_TAG} .



run:
	export FETCHER_IMAGE_TAG="local" \
	&& export fetcher_ftp_user="test" \
	&& export fetcher_ftp_pass="test" \
  && export fetcher_ftp_ip="127.0.0.1" \
	&& cd fetcher-web \
	&& ./gradlew check build \
	&& docker build -t bibletranslationtools/fetcher:$${FETCHER_IMAGE_TAG} . \
	&& cd ../fetcher-pipeline \
	&& docker build -t bibletranslationtools/fetcher-pipeline:$${FETCHER_IMAGE_TAG} . \
	&& cd ../dockerstack \
	&& docker-compose build fileserver \
	&& docker-compose up

stop:
	cd dockerstack \
	&& docker-compose down -v
