#!/bin/sh
set -eu

PORT="${PORT:-8080}"
NGINX_CONF_TEMPLATE="${NGINX_CONF_TEMPLATE:-nginx.static.conf.template}"

export PORT

echo "Starting football-client nginx on port ${PORT}"
echo "Using nginx template ${NGINX_CONF_TEMPLATE}"

envsubst '${PORT}' < "/etc/nginx/${NGINX_CONF_TEMPLATE}" > /etc/nginx/conf.d/default.conf
nginx -t
exec nginx -g 'daemon off;'
