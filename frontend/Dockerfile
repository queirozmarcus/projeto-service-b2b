# Multi-stage build: Node.js LTS for build, minimal image for runtime
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /build

# Copy package files
COPY package*.json ./

# Install dependencies (--legacy-peer-deps for @testing-library/react React 19 compatibility)
RUN npm ci --production=false --legacy-peer-deps

# Copy source code
COPY . .

# Build Next.js application
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

RUN npm run build && mkdir -p /build/public

# Stage 2: Runtime (Node.js minimal + production dependencies only)
FROM node:20-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 nextjs && \
    adduser -D -u 1001 -G nextjs nextjs

# Copy package files and install production dependencies
COPY --from=builder /build/package*.json ./
RUN npm ci --production --legacy-peer-deps && \
    npm cache clean --force

# Copy Next.js build artifacts
COPY --from=builder /build/.next ./.next
COPY --from=builder /build/next.config.js ./
COPY --from=builder /build/public ./public

# Change ownership
RUN chown -R nextjs:nextjs /app

# Switch to non-root user
USER nextjs

# Expose port
EXPOSE 3000

# Environment variables
ENV NODE_ENV=production
ENV PORT=3000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:3000 || exit 1

# Start Next.js application
CMD ["npm", "start"]
