# Stage 1: Build Angular application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source code
COPY . .

# Build Angular application for production
RUN npm run build

# Stage 2: Serve with Nginx
FROM docker.io/nginxinc/nginx-unprivileged

# Copy built application from build stage
COPY --from=build /app/dist/AccelConnectFront/browser /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 8080
EXPOSE 8080

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
