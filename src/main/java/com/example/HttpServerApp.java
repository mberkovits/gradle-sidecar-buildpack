package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpServerApp {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Create the messages endpoint
        server.createContext("/api/messages", new MessagesHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("HTTP Server started on port " + PORT);
        System.out.println("Send POST requests to http://localhost:" + PORT + "/api/messages");
        System.out.println("Expected JSON format: {\"message\": \"your message here\"}");
    }

    static class MessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                // Read the request body
                InputStream requestBody = exchange.getRequestBody();
                String body = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
                
                // Simple JSON parsing for "message" field
                String message = extractMessage(body);
                
                // Create response
                String response;
                if ("ping".equals(message)) {
                    response = "{\"response\": \"pong\"}";
                } else {
                    response = "{\"response\": \"" + escapeJson(message) + "\"}";
                }
                
                sendResponse(exchange, 200, response);
                
            } catch (Exception e) {
                System.err.println("Error processing request: " + e.getMessage());
                sendResponse(exchange, 400, "{\"error\": \"Invalid JSON or missing message field\"}");
            }
        }
        
        private String extractMessage(String json) {
            // Simple JSON parsing - look for "message":"value"
            int messageIndex = json.indexOf("\"message\"");
            if (messageIndex == -1) {
                throw new IllegalArgumentException("Missing message field");
            }
            
            int colonIndex = json.indexOf(":", messageIndex);
            if (colonIndex == -1) {
                throw new IllegalArgumentException("Invalid JSON format");
            }
            
            int startQuote = json.indexOf("\"", colonIndex);
            if (startQuote == -1) {
                throw new IllegalArgumentException("Invalid JSON format");
            }
            
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) {
                throw new IllegalArgumentException("Invalid JSON format");
            }
            
            return json.substring(startQuote + 1, endQuote);
        }
        
        private String escapeJson(String input) {
            return input.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}
