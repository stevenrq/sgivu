FROM python:3.12-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    libopenblas-dev && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# Cache mount: pip reutiliza paquetes entre builds sin redescargar
RUN --mount=type=cache,target=/root/.pip \
    pip install --cache-dir /root/.pip --upgrade pip && \
    pip install --cache-dir /root/.pip -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["bash", "./run.sh"]
