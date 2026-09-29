FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY MockServices.java MonitorServer.java ./
COPY web ./web
RUN javac MockServices.java MonitorServer.java
EXPOSE 8081
CMD ["sh", "-c", "java MockServices & java MonitorServer"]