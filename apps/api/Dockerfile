FROM node:20-alpine AS builder
WORKDIR /app
RUN npm config set registry https://registry.npmmirror.com
COPY package.json tsconfig.json nest-cli.json ./
RUN npm install --include=dev
COPY src/ ./src/
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
RUN addgroup -S manjie && adduser -S manjie -G manjie
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY package.json ./
USER manjie
EXPOSE 3000
CMD ["node", "dist/main"]
