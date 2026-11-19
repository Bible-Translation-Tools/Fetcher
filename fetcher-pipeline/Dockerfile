# Debian 13 (trixie) slim base with Python 3.12 (pydub still needs the audioop module removed in 3.13)
FROM python:3.12-slim-trixie

# Runtime user. UID/GID 105:106 match the "ftp" user in the ftp service (bogem/ftp), which
# chowns the shared content volume to that owner on every start and writes uploads as it.
# The pipeline must be able to modify those uploads, so it runs as the same owner.
ARG APP_UID=105
ARG APP_GID=106

# Distro packages only (patched through Debian security updates):
#  - openjdk-21-jre-headless replaces the unpatched OpenJDK 11.0.2 tarball (tools/*.jar target Java <= 9)
#  - supervisor replaces Node.js + pm2 as the process manager
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
        ffmpeg \
        git \
        openjdk-21-jre-headless \
        supervisor \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" app \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" --create-home --shell /usr/sbin/nologin app \
    && mkdir -p /content /repos \
    && chown app:app /content /repos

WORKDIR /fetcher-pipeline
COPY requirements.txt /fetcher-pipeline/
RUN pip install --no-cache-dir --root-user-action=ignore -r requirements.txt

# Set via compose.local.yml for local debugging only
ARG debugpy
ENV DEBUGPY=${debugpy}
RUN if [ -n "$DEBUGPY" ]; then pip install --no-cache-dir --root-user-action=ignore debugpy; fi

COPY . /fetcher-pipeline
COPY supervisord.conf /etc/supervisor/fetcher.conf
COPY entrypoint.sh /entrypoint.sh
COPY gl_repo_urls.txt /repourls/gl_repo_urls.txt
RUN chmod 0755 /entrypoint.sh

ENV PYTHONUNBUFFERED=1
USER app
ENTRYPOINT ["/entrypoint.sh"]
