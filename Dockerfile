FROM alpine:3.20

RUN echo "This repository does not use the root Dockerfile to build an app image." && \
    echo "Use one of: Dockerfile.auth-service, Dockerfile.product-service, Dockerfile.order-service, Dockerfile.gateway-service, Dockerfile.frontend" && \
    echo "Or run: docker compose build" && \
    exit 1
