# Multi-stage build: Node.js LTS for build, standalone output for runtime
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /build

# Copy package files
COPY package*.json ./

# Install all dependencies (--legacy-peer-deps for @testing-library/react React 19 compatibility)
RUN npm ci --legacy-peer-deps

# Copy source code
COPY . .

# NEXT_PUBLIC_* variables are inlined at build time — must be passed as ARG
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

# Build Next.js application (output: standalone)
RUN npm run build

# Stage 2: Runtime — uses standalone output (no full node_modules required)
FROM node:20-alpine AS runner

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S nextjs && \
    adduser -u 1001 -S nextjs -G nextjs

# Standalone output includes its own node_modules copy
COPY --from=builder --chown=nextjs:nextjs /build/.next/standalone ./
# Static assets and public folder must be copied separately
COPY --from=builder --chown=nextjs:nextjs /build/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nextjs /build/public ./public

USER nextjs

EXPOSE 3000

ENV NODE_ENV=production
ENV PORT=3000
# Hostname must be 0.0.0.0 so the container is reachable from outside
ENV HOSTNAME=0.0.0.0

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:3000 || exit 1

# standalone output generates server.js at root of /app
CMD ["node", "server.js"]
