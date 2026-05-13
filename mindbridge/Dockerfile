FROM node:22-alpine AS build
WORKDIR /app

<<<<<<< HEAD
# Enable corepack
RUN corepack enable && corepack prepare pnpm@latest --activate

# Copy dependency files
COPY package.json pnpm-lock.yaml* ./

# Install dependencies using pnpm, but ignore scripts initially so it doesn't fail
RUN pnpm install --ignore-scripts --no-frozen-lockfile

# Force npm to rebuild the specific package that requires it
RUN npm rebuild sharp

# Copy source code and build Next.js
COPY . .
RUN pnpm build

FROM node:22-alpine
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@latest --activate
ENV NODE_ENV=production

# Copy built assets
COPY --from=build /app/package.json ./
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/node_modules ./node_modules

EXPOSE 3000
CMD ["pnpm", "start"]
=======
COPY package.json pnpm-lock.yaml* ./
RUN corepack enable && pnpm install --ignore-scripts

COPY . .

ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL

RUN pnpm build

FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

COPY --from=build /app ./

EXPOSE 3000
CMD ["corepack", "pnpm", "start"]
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3
