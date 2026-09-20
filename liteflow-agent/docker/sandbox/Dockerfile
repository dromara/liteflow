FROM node:22-bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive \
    NPM_CONFIG_PREFIX=/home/node/.npm-global \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        build-essential \
        bzip2 \
        ca-certificates \
        curl \
        dnsutils \
        fd-find \
        file \
        git \
        iproute2 \
        iputils-ping \
        jq \
        less \
        lsof \
        netcat-openbsd \
        openssh-client \
        pkg-config \
        procps \
        python3 \
        python3-dev \
        python3-pip \
        python3-venv \
        ripgrep \
        rsync \
        sqlite3 \
        tree \
        unzip \
        wget \
        xz-utils \
        zip \
    && rm -rf /var/lib/apt/lists/* \
    && python3 -m venv /opt/venv \
    && ln -s /usr/bin/fdfind /usr/local/bin/fd \
    && install -d -o node -g node \
        /workspace \
        /home/node/.cache \
        /home/node/.npm-global \
    && chown -R node:node /opt/venv

ENV VIRTUAL_ENV=/opt/venv \
    PATH="/opt/venv/bin:/home/node/.npm-global/bin:${PATH}"

COPY sandbox-path.sh /etc/profile.d/sandbox-path.sh

USER node
WORKDIR /workspace

CMD ["/bin/bash"]
