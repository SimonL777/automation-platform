FROM mcr.microsoft.com/playwright:v1.63.0-noble
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts
COPY contract.mjs run.mjs ./
RUN chmod 0444 /app/contract.mjs /app/run.mjs /app/package*.json
USER pwuser
CMD ["node","/app/run.mjs"]
