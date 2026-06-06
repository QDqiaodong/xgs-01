ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/node:20-alpine AS builder

WORKDIR /app

COPY .npmrc ./
COPY package*.json ./

RUN npm install --no-audit --no-fund

COPY . .

RUN npm run build

FROM ${DOCKER_REGISTRY}/library/nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
