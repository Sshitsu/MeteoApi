# Практическое задание. Проект «Сервис по получению метеорологических данных» (семестровое задание). Программирование на языке Java

Для запуска проекта, необходимо установить переменную окружения YANDEX_WEATHER_KEY

Cmd: `set YANDEX_WEATHER_KEY=ваш_ключ`
Pwsh: `$env:YANDEX_WEATHER_KEY="ваш_ключ"`

Запуск проекта: `mvn -q compile exec:java "-Dexec.args=lat lon limit"`
