FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.27-alpine
ARG NGINX_CONF_TEMPLATE=nginx.static.conf.template
ENV NGINX_CONF_TEMPLATE=${NGINX_CONF_TEMPLATE}
COPY nginx.conf.template /etc/nginx/nginx.conf.template
COPY nginx.static.conf.template /etc/nginx/nginx.static.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
CMD ["/bin/sh", "-c", ": ${PORT:=8080}; export PORT; envsubst '${PORT}' < /etc/nginx/${NGINX_CONF_TEMPLATE} > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
