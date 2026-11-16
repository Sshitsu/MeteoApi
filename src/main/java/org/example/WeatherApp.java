package org.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Locale;

public class WeatherApp {
    private static final String BASE_URL = "https://api.weather.yandex.ru/v2/forecast";
    private static final String KEY_HEADER = "X-Yandex-Weather-Key";

    public static void main(String[] args) throws IOException, InterruptedException {
        var validatedArgs = CliArgs.parse(args);
        var apiKey = getApiKey();

        var request = buildRequest(validatedArgs, apiKey);
        var response = fetchApi(request);
        validateResponseFromService(response);

        String body = response.body();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        System.out.println("Полный ответ сервиса:");
        System.out.println(body);

        System.out.println();
        System.out.println("Текущая температура (fact.temp): " + formatTemp(currentTemp(json)));

        System.out.println();
        System.out.println("Средняя температура за " + validatedArgs.limit() + " сут. (parts.day.temp_avg): "
                + formatTemp(averageTempByLimit(json, validatedArgs.limit())));
    }

    private static double currentTemp(JsonObject json) {
        JsonObject fact = json.getAsJsonObject("fact");
        if (fact == null || !fact.has("temp")) {
            throw new IllegalStateException("В ответе нет поля fact.temp");
        }
        return fact.get("temp").getAsDouble();
    }

    private static double averageTempByLimit(JsonObject json, int rawLimit) {
        var forecasts = json.getAsJsonArray("forecasts");
        if (forecasts == null || forecasts.isEmpty()) {
            throw new IllegalStateException("В ответе нет массива forecasts");
        }

        int limit = Math.min(rawLimit, forecasts.size());
        double sum = 0.0;
        long count = 0;

        for (int i = 0; i < limit; i++) {
            JsonElement dayTemp = dayTempAvg(forecasts.get(i).getAsJsonObject());
            if (dayTemp != null) {
                sum += dayTemp.getAsDouble();
                count++;
            }
        }
        if (count == 0) {
            throw new IllegalStateException("Ни в одном прогнозе нет parts.day.temp_avg");
        }

        return sum / count;
    }

    private static JsonElement dayTempAvg(JsonObject forecast) {
        JsonObject parts = forecast.getAsJsonObject("parts");
        if (parts == null) {
            return null;
        }
        JsonObject day = parts.getAsJsonObject("day");
        if (day == null || !day.has("temp_avg")) {
            return null;
        }
        return day.get("temp_avg");
    }

    private static void validateResponseFromService(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            System.err.println("Сервис вернул код " + response.statusCode() + ":");
            System.err.println(response.body());
            System.exit(1);
        }
    }

    private static HttpRequest buildRequest(CliArgs args, String apiKey) {
        String url = String.format(Locale.US, "%s?lat=%s&lon=%s&limit=%d",
                BASE_URL, args.lat(), args.lon(), args.limit());
        return HttpRequest.newBuilder(URI.create(url))
                .header(KEY_HEADER, apiKey)
                .GET()
                .build();
    }

    private static HttpResponse<String> fetchApi(HttpRequest request) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String getApiKey() {
        String apiKey = System.getenv("YANDEX_WEATHER_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Не задан ключ API: установите переменную окружения YANDEX_WEATHER_KEY");
            System.exit(1);
        }
        return apiKey.trim();
    }

    private record CliArgs(double lat, double lon, int limit) {
        static CliArgs parse(String[] args) {
            try {
                if (args.length != 3) {
                    System.err.println("Укажите 3 аргумента: lat lon limit, например: 58.25 40.62 7");
                    System.exit(1);
                }
                int limit = Integer.parseInt(args[2]);
                if (limit < 1) {
                    System.err.println("limit должен быть положительным числом суток");
                    System.exit(1);
                }
                return new CliArgs(Double.parseDouble(args[0]), Double.parseDouble(args[1]), limit);
            } catch (NumberFormatException e) {
                System.err.println("Аргументы программы должны быть числами" + Arrays.toString(args));
                System.exit(1);
            }
            return null;
        }
    }

    private static String formatTemp(double temp) {
        return String.format(Locale.US, "%.1f °C", temp);
    }
}
