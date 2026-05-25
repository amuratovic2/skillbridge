FROM node:22-alpine AS build
WORKDIR /workspace

ENV CI=true
RUN corepack enable && corepack prepare pnpm@10.33.0 --activate
COPY package.json pnpm-lock.yaml nx.json tsconfig.base.json eslint.config.mjs ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store pnpm install --frozen-lockfile --config.confirmModulesPurge=false

COPY apps/web apps/web
ARG VITE_API_URL=http://localhost:3000/api
ENV VITE_API_URL=$VITE_API_URL
RUN pnpm build

FROM nginx:1.27-alpine
COPY docker/nginx-web.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/apps/web /usr/share/nginx/html
EXPOSE 80
