package com.uap.proiv.jobs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uap.proiv.jobs.client.UserApiRepository;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración de AssignController.
 * Integra AssignController -> UserJobAssignedServiceImpl -> (JobServiceImpl + jobs.json real)
 *                                                        -> (UserServiceImpl -> UserApiRepository)
 *                                                        -> AssignedServiceImpl
 * Solo se reemplaza la API externa ReqRes por un MockWebServer que simula sus 2 páginas de usuarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AssignControllerIntegrationTest {

    private static final String API_KEY = "free_user_3HYTiqu2JKQ4TfGq884xW5mqfrd";
    private static final MockWebServer mockWebServer = new MockWebServer();

    /** Si es true, la API externa simulada responde 500 a todo. */
    private static volatile boolean apiExternaCaida = false;

    static {
        mockWebServer.setDispatcher(new ReqResDispatcher());
        try {
            mockWebServer.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        UserApiRepository userApiRepositoryMockWebServer(ObjectMapper objectMapper) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            String baseUrl = mockWebServer.url("/api/users").toString();
            return new UserApiRepository(httpClient, objectMapper, baseUrl, API_KEY);
        }
    }

    @BeforeEach
    void limpiarEstado() throws InterruptedException {
        apiExternaCaida = false;
        // descarta requests registrados por tests anteriores
        while (mockWebServer.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // vacío
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ------------------------------------------------------------------ casos

    @Test
    @DisplayName("POST /api/assign integración completa: asigna usuarios a los 7 trabajos de jobs.json")
    void assign_success() throws Exception {
        String body = """
                { "requestNumber": 1001, "clientName": "UAP" }
                """;

        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Client").value("UAP"))
                .andExpect(jsonPath("$.Request_Number").value(1001))
                .andExpect(jsonPath("$.Assign.length()").value(7))
                // se respeta el orden del catálogo y cada trabajo recibe tantos usuarios como "resources"
                .andExpect(jsonPath("$.Assign[0].job.name").value("Data Engineer"))
                .andExpect(jsonPath("$.Assign[0].users.length()").value(3))
                .andExpect(jsonPath("$.Assign[1].users.length()").value(3))
                .andExpect(jsonPath("$.Assign[2].job.name").value("Backend Engineer"))
                .andExpect(jsonPath("$.Assign[2].users.length()").value(5))
                .andExpect(jsonPath("$.Assign[3].users.length()").value(2))
                .andExpect(jsonPath("$.Assign[4].users.length()").value(3))
                .andExpect(jsonPath("$.Assign[5].users.length()").value(3))
                .andExpect(jsonPath("$.Assign[6].job.name").value("Full Stack Developer"))
                .andExpect(jsonPath("$.Assign[6].users.length()").value(10))
                // los usuarios vienen de la API externa (ids 1..12) con el formato de ReqRes
                .andExpect(jsonPath("$.Assign[0].users[0].id").isNumber())
                .andExpect(jsonPath("$.Assign[0].users[0].email").value(containsString("@reqres.in")))
                .andExpect(jsonPath("$.Assign[0].users[0].first_name").isString())
                .andExpect(jsonPath("$.Assign[0].users[0].last_name").isString());

        // el servicio recorre las páginas 1, 2 y 3 (la 3 viene vacía y corta el ciclo)
        RecordedRequest page1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest page2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest page3 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/api/users?page=1", page1.getPath());
        assertEquals("/api/users?page=2", page2.getPath());
        assertEquals("/api/users?page=3", page3.getPath());
        assertEquals("application/json", page1.getHeader("Accept"));
        assertEquals(API_KEY, page1.getHeader("X-API-KEY"));
        assertNull(mockWebServer.takeRequest(1, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("POST /api/assign integración - API externa caída retorna 500 con el error")
    void assign_apiExternaCaida() throws Exception {
        apiExternaCaida = true;

        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"requestNumber\": 1, \"clientName\": \"UAP\" }"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error al conectar con la API de usuarios")))
                .andExpect(content().string(containsString("500")));
    }

    @Test
    @DisplayName("POST /api/assign integración - sin clientName retorna 400 y no llama a la API externa")
    void assign_sinClientName() throws Exception {
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"requestNumber\": 1 }"))
                .andExpect(status().isBadRequest());

        assertNull(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("POST /api/assign integración - sin requestNumber retorna 400")
    void assign_sinRequestNumber() throws Exception {
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientName\": \"UAP\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/assign integración - body con JSON inválido retorna 400")
    void assign_jsonInvalido() throws Exception {
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ requestNumber: "))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ API externa simulada

    /** Simula ReqRes: 12 usuarios en 2 páginas de 6, la página 3 vacía. */
    static class ReqResDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            if (apiExternaCaida) {
                return new MockResponse().setResponseCode(500).setBody("{\"error\":\"down\"}");
            }
            String path = request.getPath() == null ? "" : request.getPath();
            int page = path.contains("page=") ? Integer.parseInt(path.substring(path.indexOf("page=") + 5)) : 1;
            return new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody(pagina(page));
        }

        private static String pagina(int page) {
            int perPage = 6;
            int total = 12;
            List<String> data = new ArrayList<>();
            for (int id = (page - 1) * perPage + 1; id <= Math.min(page * perPage, total); id++) {
                data.add("""
                        {"id": %d, "email": "user%d@reqres.in", "first_name": "Nombre%d", "last_name": "Apellido%d", "avatar": "https://reqres.in/img/faces/%d-image.jpg"}"""
                        .formatted(id, id, id, id, id));
            }
            return """
                    {"page": %d, "per_page": %d, "total": %d, "total_pages": 2, "data": [%s]}"""
                    .formatted(page, perPage, total, String.join(",", data));
        }
    }
}
