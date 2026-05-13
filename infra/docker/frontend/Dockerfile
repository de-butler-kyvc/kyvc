FROM node:20-alpine AS frontend_build
WORKDIR /workspace
ARG KYVC_ENV=dev

COPY frontend ./frontend

RUN set -e; \
    if [ -f frontend/package.json ]; then \
      cd frontend; \
      if [ -f package-lock.json ]; then npm ci; else npm install; fi; \
      echo "Building frontend with KYVC_ENV=${KYVC_ENV:-dev}"; \
      NEXT_PUBLIC_KYVC_ENV="${KYVC_ENV:-dev}" npm run build; \
      if [ -d out ]; then \
        echo "frontend build output found: out"; \
        cp -a out /workspace/frontend-build; \
      elif [ -d dist ]; then \
        echo "frontend build output found: dist"; \
        cp -a dist /workspace/frontend-build; \
      else \
        echo "frontend build output not found"; \
        echo "current directory:"; \
        pwd; \
        echo "files:"; \
        ls -al; \
        echo "package scripts:"; \
        cat package.json; \
        echo "next config files:"; \
        ls -al next.config.* 2>/dev/null || true; \
        exit 1; \
      fi; \
    else \
      mkdir -p /workspace/frontend-build && \
      echo '<!doctype html><html><head><meta charset="utf-8"><title>KYvC Frontend</title></head><body><h1>KYvC Frontend</h1><p>Frontend placeholder is running.</p></body></html>' > /workspace/frontend-build/index.html; \
    fi

FROM node:20-alpine AS frontend_admin_build
WORKDIR /workspace
ARG KYVC_ENV=dev
ARG FRONTEND_ADMIN_API_BASE_URL

COPY frontend_admin ./frontend_admin

RUN set -e; \
    if [ -f frontend_admin/package.json ]; then \
      cd frontend_admin; \
      if [ -f package-lock.json ]; then npm ci; else npm install; fi; \
      echo "Building frontend_admin with KYVC_ENV=${KYVC_ENV:-dev}"; \
      ADMIN_API_BASE="${FRONTEND_ADMIN_API_BASE_URL:-}"; \
      if [ -z "$ADMIN_API_BASE" ] && [ "${KYVC_ENV:-dev}" = "dev" ]; then ADMIN_API_BASE="https://dev-admin-api-kyvc.khuoo.synology.me"; fi; \
      NEXT_PUBLIC_KYVC_ENV="${KYVC_ENV:-dev}" NEXT_PUBLIC_ADMIN_API_BASE_URL="$ADMIN_API_BASE" npm run build; \
      if [ -d out ]; then \
        echo "frontend_admin build output found: out"; \
        cp -a out /workspace/frontend-admin-build; \
      elif [ -d dist ]; then \
        echo "frontend_admin build output found: dist"; \
        cp -a dist /workspace/frontend-admin-build; \
      else \
        echo "frontend_admin build output not found"; \
        echo "current directory:"; \
        pwd; \
        echo "files:"; \
        ls -al; \
        echo "package scripts:"; \
        cat package.json; \
        echo "next config files:"; \
        ls -al next.config.* 2>/dev/null || true; \
        exit 1; \
      fi; \
    else \
      mkdir -p /workspace/frontend-admin-build && \
      echo '<!doctype html><html><head><meta charset="utf-8"><title>KYvC Admin</title></head><body><h1>KYvC Admin</h1><p>Backend Admin Frontend placeholder is running.</p></body></html>' > /workspace/frontend-admin-build/index.html; \
    fi

FROM node:20-alpine AS frontend_core_admin_build
WORKDIR /workspace
ARG KYVC_ENV=dev

COPY frontend_core_admin ./frontend_core_admin

RUN set -e; \
    cd frontend_core_admin; \
    if [ -f package-lock.json ]; then npm ci; else npm install; fi; \
    echo "Building frontend_core_admin with KYVC_ENV=${KYVC_ENV:-dev}"; \
    NEXT_PUBLIC_KYVC_ENV="${KYVC_ENV:-dev}" npm run build; \
    if [ -d out ]; then \
      echo "frontend_core_admin build output found: out"; \
      cp -a out /workspace/frontend-core-admin-build; \
    elif [ -d dist ]; then \
      echo "frontend_core_admin build output found: dist"; \
      cp -a dist /workspace/frontend-core-admin-build; \
    else \
      echo "frontend_core_admin build output not found"; \
      echo "current directory:"; \
      pwd; \
      echo "files:"; \
      ls -al; \
      echo "package scripts:"; \
      cat package.json; \
      echo "next config files:"; \
      ls -al next.config.* 2>/dev/null || true; \
      exit 1; \
    fi

FROM nginx:1.27-alpine

ARG NGINX_CONF=dev.conf

COPY infra/nginx/frontend/${NGINX_CONF} /etc/nginx/conf.d/default.conf

COPY --from=frontend_build /workspace/frontend-build /usr/share/nginx/html/frontend
COPY --from=frontend_admin_build /workspace/frontend-admin-build /usr/share/nginx/html/frontend_admin
COPY --from=frontend_core_admin_build /workspace/frontend-core-admin-build /usr/share/nginx/html/frontend_core_admin

EXPOSE 80
