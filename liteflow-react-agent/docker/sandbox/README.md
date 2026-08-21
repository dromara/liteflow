# LiteFlow Agent Docker Sandbox

This image provides a reusable Docker execution environment for
`liteflow-react-agent-harness`.

## Build

From any working directory, run:

```bash
./liteflow-react-agent/docker/sandbox/build.sh
```

The default image tag is `liteflow-agent-sandbox:node22`. Pass a different tag as the only
argument when required:

```bash
./liteflow-react-agent/docker/sandbox/build.sh registry.example.com/liteflow-agent-sandbox:node22
```

## Runtime environment

- Node.js 22 and npm;
- Python 3, pip, venv, development headers, and native build tools;
- curl, wget, Git, jq, ripgrep, fd, SQLite, rsync, and archive tools;
- DNS, IP, socket, process, and file diagnostics;
- writable `/workspace`, `/opt/venv`, and user-scoped npm global directory;
- non-root `node` user by default.

Python packages installed with `pip install` use `/opt/venv`. Packages installed with
`npm install -g` use `/home/node/.npm-global`. Add operating-system packages to the Dockerfile
and rebuild the image instead of installing them with elevated privileges at runtime.
