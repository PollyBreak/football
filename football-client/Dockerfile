FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.27-alpine
ENV PORT=80
COPY nginx.conf.template /etc/nginx/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["/bin/sh", "-c", "envsubst '${PORT}' < /etc/nginx/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
