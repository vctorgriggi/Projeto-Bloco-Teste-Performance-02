# syntax=docker/dockerfile:1.7
#
# imagem do front-end: o build do vite gera arquivos estaticos, e quem os serve em
# producao e o nginx -- nao o servidor de desenvolvimento do vite.

FROM node:26-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# a variante unprivileged roda o nginx sem root e escuta na 8080, e nao na 80
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /build/dist /usr/share/nginx/html
ENV BLOG_API_URL=http://blog-api:8080
EXPOSE 8080
