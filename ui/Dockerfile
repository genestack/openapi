FROM nginx

COPY ui/node_modules/openapi-explorer/dist/browser/openapi-explorer.min.js /usr/share/nginx/html
COPY ui/index.html /usr/share/nginx/html
COPY openapi/v1/odmApi.yaml /usr/share/nginx/html
COPY openapi/v1/schemas /usr/share/nginx/html/schemas
