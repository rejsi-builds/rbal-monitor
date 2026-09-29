import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

public class MockServices {

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        List<BankService> services = List.of(
                new BankService("DEV ATM Network API", 8082),
                new BankService("DEV Mobile Banking Backend", 8083),
                new BankService("DEV Card Processor", 8084),
                new BankService("UAT ATM Network API", 8085),
                new BankService("UAT Mobile Banking Backend", 8086),
                new BankService("UAT Card Processor", 8087),
                new BankService("PROD ATM Network API", 8088),
                new BankService("PROD Mobile Banking Backend", 8089),
                new BankService("PROD Card Processor", 8090)
        );

        for (BankService service : services) {
            service.start();
        }

        runChaos(services);
    }

    private static void runChaos(List<BankService> services) {
        while (true) {
            pause(5000);

            for (BankService service : services) {
                if (service.up) {
                    if (random.nextInt(100) < 5) {
                        service.up = false;
                        System.out.println("[CRASH] " + service.name + " went down");
                    }
                } else {
                    if (random.nextInt(100) < 40) {
                        service.up = true;
                        service.startedAt = System.currentTimeMillis();
                        System.out.println("[RECOVERED] " + service.name + " is back up");
                    }
                }
            }
        }
    }

    private static void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class BankService {
        String name;
        int port;
        volatile boolean up = true;
        volatile long startedAt = System.currentTimeMillis();

        BankService(String name, int port) {
            this.name = name;
            this.port = port;
        }

        void start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", new HealthHandler(this));
            server.start();
            System.out.println(name + " running on port " + port);
        }
    }

    static class HealthHandler implements HttpHandler {
        private final BankService service;

        HealthHandler(BankService service) {
            this.service = service;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            pause(random.nextInt(150) + 20);

            if (!service.up) {
                send(exchange, 503, "{\"error\": \"Service unavailable\"}");
                return;
            }

            long uptimeSeconds = (System.currentTimeMillis() - service.startedAt) / 1000;
            int cpu = random.nextInt(25) + 5;
            int ram = random.nextInt(30) + 40;

            String body = "{\"cpu\": " + cpu + ", \"ram\": " + ram + ", \"uptimeSeconds\": " + uptimeSeconds + "}";
            send(exchange, 200, body);
        }

        private void send(HttpExchange exchange, int code, String body) throws IOException {
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}