# P50: fl-service's own container image. Deliberately minimal -- this
# process is a stateless numeric transform behind a REST boundary (feature
# vectors and model parameters in, privatized deltas and model versions
# out); see docker-compose.fl.yml and V19's migration docstring for why it
# holds no Postgres credentials in this implementation.
FROM python:3.12-slim

WORKDIR /app

COPY pyproject.toml README.md ./
COPY app ./app

RUN pip install --no-cache-dir . && pip install --no-cache-dir uvicorn

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
