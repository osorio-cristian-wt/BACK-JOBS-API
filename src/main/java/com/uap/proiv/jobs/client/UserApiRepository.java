package com.uap.proiv.jobs.client;

import com.uap.proiv.jobs.dto.User;
import com.uap.proiv.jobs.dto.UserApiResponse;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserApiRepository {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public UserApiRepository() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(),
                new ObjectMapper(),
                "https://reqres.in/api/users",
                "free_user_3HYTiqu2JKQ4TfGq884xW5mqfrd");
    }

    public UserApiRepository(HttpClient httpClient, ObjectMapper objectMapper, String baseUrl, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public UserApiResponse getUsers(int page) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "?page=" + page))
                    .header("Accept", "application/json")
                    .header("X-API-KEY",  apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), UserApiResponse.class);
            } else {
                throw new RuntimeException("Error en ReqRes API. Código: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con la API de usuarios: " + e.getMessage(), e);
        }
    }

    public User getUserById(int id) {
        return getUsers(1).getData().stream().filter(user -> id == user.getId()).findFirst().orElse(null);
    }
/*
    public User getUserById(int id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + id))
                    .header("Accept", "application/json")
                    .header("X-API-KEY",  apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), User.class);
            } else {
                throw new RuntimeException("Error en ReqRes API. Código: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con la API de usuarios: " + e.getMessage(), e);
        }
    }
*/
    public void updateUser(User user) {
        try {
            Map<String, String> userMap = new HashMap<>();
            userMap.put("name", user.getFirstName()); //success value only "morpheus"
            userMap.put("job",user.getLastName()); //succes value only "zion resident"
            String requestBody = objectMapper.writeValueAsString(userMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + user.getId()))
                    .header("Accept", "application/json")
                    .header("X-API-KEY", apiKey)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Error en ReqRes API. Código: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con la API de usuarios: " + e.getMessage(), e);
        }
    }

}

