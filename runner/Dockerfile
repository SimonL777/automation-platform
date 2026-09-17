FROM mcr.microsoft.com/playwright:v1.63.0-noble
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts
COPY contract.mjs run.mjs ./
USER pwuser
CMD ["node","/app/run.mjs"]
