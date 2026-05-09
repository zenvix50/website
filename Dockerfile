FROM node:22.12.0

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN cd client && npm install && npm run build

EXPOSE 3000

CMD ["npm", "start"]