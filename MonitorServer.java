import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MonitorServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/", new StaticHandler());
        server.start();
        System.out.println("RBAL Monitor running on http://localhost:8081");
    }

    // Serves the dashboard files from the "web" folder
    static class StaticHandler implements HttpHandler {
        private final List<String> allowedFiles = List.of("index.html", "style.css", "sc.js", "rbal-logo.png");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            String fileName = path.substring(1);

            File file = new File("web", fileName);
            if (!allowedFiles.contains(fileName) || !file.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().add("Content-Type", getContentType(fileName));
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".html")) {
                return "text/html; charset=utf-8";
            }
            if (fileName.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (fileName.endsWith(".js")) {
                return "application/javascript; charset=utf-8";
            }
            return "image/png";
        }
    }

    static class StatusHandler implements HttpHandler {
        private final HttpClient client = HttpClient.newHttpClient();

        private final List<ServiceConfig> services = List.of(
                new ServiceConfig("ATM Network API", "DEV", 8082),
                new ServiceConfig("Mobile Banking Backend", "DEV", 8083),
                new ServiceConfig("Card Processor", "DEV", 8084),
                new ServiceConfig("ATM Network API", "UAT", 8085),
                new ServiceConfig("Mobile Banking Backend", "UAT", 8086),
                new ServiceConfig("Card Processor", "UAT", 8087),
                new ServiceConfig("ATM Network API", "PROD", 8088),
                new ServiceConfig("Mobile Banking Backend", "PROD", 8089),
                new ServiceConfig("Card Processor", "PROD", 8090)
        );

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            StringBuilder json = new StringBuilder();
            json.append("{\"lastUpdate\": \"").append(timestamp).append("\", \"services\": [");

            for (int i = 0; i < services.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append(checkService(services.get(i)));
            }

            json.append("]}");

            byte[] response = json.toString().getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }

        private String checkService(ServiceConfig service) {
            String url = "http://localhost:" + service.port();
            String status = "DOWN";
            String metrics = "{}";
            long startTime = System.currentTimeMillis();

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/health"))
                        .timeout(Duration.ofMillis(1500))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    status = "UP";
                    metrics = response.body();
                }
            } catch (Exception e) {
                // Service did not answer, so it stays DOWN
            }

            long responseMs = System.currentTimeMillis() - startTime;

            return "{\"name\": \"" + service.name() + "\", "
                    + "\"env\": \"" + service.env() + "\", "
                    + "\"url\": \"" + url + "\", "
                    + "\"status\": \"" + status + "\", "
                    + "\"responseMs\": " + responseMs + ", "
                    + "\"metrics\": " + metrics + "}";
        }
    }

    record ServiceConfig(String name, String env, int port) {}
}