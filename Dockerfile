FROM openjdk:8-alpine

COPY target/uberjar/minesweeper.jar /minesweeper/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/minesweeper/app.jar"]
