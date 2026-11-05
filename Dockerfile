FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY src ./src
COPY web ./web
RUN javac -d out $(find src -name "*.java")
ENV PORT=10000
EXPOSE 10000
CMD ["java", "-cp", "out", "librarysystem.WebServer"]
