# ──────────────────────────────────────────────
# Stage 1: dependency builder
# ──────────────────────────────────────────────
FROM python:3.12-slim AS builder

WORKDIR /build

# Install build tools needed for some ML dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# Pre-download the detoxify 'original' model so the container does not need
# outbound internet access at runtime. The model is cached in the default
# torch/transformers cache directory and baked into the image.
COPY app/ app/
RUN python -c "from detoxify import Detoxify; Detoxify('original')"

# ──────────────────────────────────────────────
# Stage 2: runtime image
# ──────────────────────────────────────────────
FROM python:3.12-slim AS runtime

WORKDIR /app

# Copy installed packages and pre-cached model weights
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin
COPY --from=builder /root/.cache /root/.cache
COPY app/ app/

# Non-root user for security
RUN useradd -m appuser
USER appuser

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
