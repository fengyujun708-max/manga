FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json next.config.js tsconfig.json ./
RUN npm install

COPY src/ ./src/
COPY public/ ./public/
RUN npm run build

FROM node:20-alpine AS runner

WORKDIR /app

RUN addgroup -S manjie && adduser -S manjie -G manjie

COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

USER manjie

EXPOSE 3001

ENV PORT=3001
ENV NODE_ENV=production

CMD ["node", "server.js"]