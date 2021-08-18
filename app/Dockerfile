FROM openjdk:11-jre-slim
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/app-shadow/ /app/
WORKDIR /app/bin
CMD ["./app"]