
# Используем образ с JDK 21
FROM openjdk:21-jdk-slim

# Указываем рабочую директорию в контейнере
WORKDIR /app

# Копируем собранный JAR-файл в контейнер
COPY target/lemana-practice-tgbot-1.0-SNAPSHOT.jar app.jar

# Открываем порт, на котором будет работать приложение
EXPOSE 8080

# Команда для запуска приложения
CMD ["java", "-jar", "/app/app.jar"]
