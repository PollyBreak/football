#!/bin/sh
set -eu

PORT="${PORT:-8080}"
NGINX_CONF_TEMPLATE="${NGINX_CONF_TEMPLATE:-nginx.static.conf.template}"
EXTRA_LISTEN=""

if [ "${PORT}" != "80" ]; then
  EXTRA_LISTEN="    listen 80;"
fi

export PORT
export EXTRA_LISTEN

echo "Starting football-client nginx on port ${PORT}"
if [ -n "${EXTRA_LISTEN}" ]; then
  echo "Also listening on port 80 for Railway public domain fallback"
fi
echo "Using nginx template ${NGINX_CONF_TEMPLATE}"

envsubst '${PORT} ${EXTRA_LISTEN}' < "/etc/nginx/${NGINX_CONF_TEMPLATE}" > /etc/nginx/conf.d/default.conf
nginx -t
exec nginx -g 'daemon off;'
