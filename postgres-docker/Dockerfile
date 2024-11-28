# Используем официальный образ PostgreSQL
FROM postgres:15

# Указываем метаданные о Dockerfile
LABEL authors="Слава"

# Устанавливаем переменные окружения для базы данных
ENV POSTGRES_DB=data_base
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=Pisemnet123

# Открываем порт PostgreSQL
EXPOSE 5432

# Устанавливаем точку входа
ENTRYPOINT ["docker-entrypoint.sh"]

# Указываем команду для запуска PostgreSQL
CMD ["postgres"]
