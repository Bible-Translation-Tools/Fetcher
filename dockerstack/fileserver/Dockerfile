# Unprivileged nginx (mainline 1.31; the 1.30 stable image lags on security fixes): runs as UID 101 and listens on 8080 (not 80)
FROM nginxinc/nginx-unprivileged:1.31-alpine-slim

# Pick up Alpine security fixes published since the upstream image was built
USER root
RUN apk upgrade --no-cache
USER 101

COPY default.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
