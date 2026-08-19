FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json ./
RUN npm config set registry https://registry.npmmirror.com && npm install

COPY tsconfig.json ./
COPY src/ ./src/
COPY prisma/ ./prisma/

RUN npm run build

FROM node:20-alpine AS runner

WORKDIR /app

RUN addgroup -S manjie && adduser -S manjie -G manjie

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./

USER manjie

CMD ["node", "dist/main.js"]