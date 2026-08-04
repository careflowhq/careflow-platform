# syntax=docker/dockerfile:1
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG API_GATEWAY_URL=http://api-gateway:8080
ENV API_GATEWAY_URL=$API_GATEWAY_URL
RUN npm run build

FROM node:20-alpine AS run
WORKDIR /app

ARG BUILD_VERSION=0.0.1-SNAPSHOT

LABEL org.opencontainers.image.title="CareFlow frontend"
LABEL org.opencontainers.image.version="${BUILD_VERSION}"
LABEL org.opencontainers.image.vendor="CareFlow HQ"
LABEL careflow.service="frontend"

ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

RUN addgroup -S careflow && adduser -S careflow -G careflow \
    && apk add --no-cache wget

COPY --from=build /app/public ./public
COPY --from=build --chown=careflow:careflow /app/.next/standalone ./
COPY --from=build --chown=careflow:careflow /app/.next/static ./.next/static

USER careflow
EXPOSE 3000
CMD ["node", "server.js"]
