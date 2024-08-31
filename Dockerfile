# Используем официальный образ с OpenJDK
FROM openjdk:17-jdk-slim

# Указываем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем файл сборки Maven или Gradle в контейнер
COPY target/your-app.jar /app/your-app.jar

# Устанавливаем команду по умолчанию для запуска приложения
ENTRYPOINT ["java", "-jar", "your-app.jar"]
