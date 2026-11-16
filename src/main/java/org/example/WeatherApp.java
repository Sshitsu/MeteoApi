package org.example;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Objects;

public class WeatherApp {
    private static final Object BASE_URL = "https://api.weather.yandex.ru/v2/forecast";
    private static final String KEY_HEADER = "X-Yandex-Weather-Key";


    static void main(String[] args) throws IOException, InterruptedException {
        var validatedArgs = new CliArgs(args);
        var apiKey = getApiKey(validatedArgs.limit);

        var request = buildRequest(validatedArgs, apiKey);
        var response = fetchApi(request);
        validateResponseFromService(response);

        String body = response.body();
        System.out.println("Полный ответ сервиса:");
        System.out.println(body);

    }


    private static void validateResponseFromService(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            System.err.println("Сервис вернул код " + response.statusCode() + ":");
            System.err.println(response.body());
            System.exit(1);
        }
    }

    private static HttpRequest buildRequest(CliArgs args, String apiKey) {
        String url = String.format(Locale.US, "%s?lat=%s&lon=%s&limit=%d", BASE_URL, args.lat, args.lon, args.limit);
        return HttpRequest.newBuilder(URI.create(url))
                .header(KEY_HEADER, apiKey)
                .GET()
                .build();
    }

    private static HttpResponse<String> fetchApi(HttpRequest request) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String getApiKey(int limit) {
        String apiKey = System.getenv("YANDEX_WEATHER_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Не задан ключ API: установите переменную окружения YANDEX_WEATHER_KEY");
            System.exit(1);
        }
        if (limit < 1) {
            System.err.println("limit должен быть положительным числом суток");
            System.exit(1);
        }
        apiKey = apiKey.toUpperCase();
        return apiKey;
    }

    private record CliArgs(double lat, double lon, int limit) {
        public CliArgs(String[] args) {
            if (args.length != 2) {
                System.err.println("Аргументы пусты, вы должны написать координаты точки lat и lon, в которой хотите определить погоду, а также limit");
                System.exit(1);
            }
            this(Double.parseDouble(args[0]), Double.parseDouble(args[1]), Integer.parseInt(args[3]));
            new CliArgs(lat, lon, limit);
        }
    }
}