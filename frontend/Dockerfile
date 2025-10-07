FROM node:20-alpine
WORKDIR /usr/share/app
COPY . .
RUN npm -g i serve
EXPOSE 80
CMD ["serve","-l","80","."]