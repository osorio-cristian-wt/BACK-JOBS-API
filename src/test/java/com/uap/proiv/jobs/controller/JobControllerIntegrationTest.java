package com.uap.proiv.jobs.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración de JobController.
 * Integra JobController -> JobServiceImpl -> JobApiRepository -> jobs.json (archivo real del classpath).
 * No hay mocks: se levanta el contexto completo de Spring y se llama al endpoint con MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/job/all integración Controller, Service, Repository y jobs.json")
    void getAllJobs_devuelveElCatalogoCompleto() throws Exception {
        // Construcción: no hace falta preparar nada (se usa el jobs.json real)
        // Prueba y verificación: GET /api/job/all responde 200 con los 7 trabajos, revisando el primero y el último
        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Data Engineer"))
                .andExpect(jsonPath("$[0].salary").value(5000.0))
                .andExpect(jsonPath("$[0].hours").value(530))
                .andExpect(jsonPath("$[0].resources").value(3))
                .andExpect(jsonPath("$[6].id").value(8))
                .andExpect(jsonPath("$[6].name").value("Full Stack Developer"))
                .andExpect(jsonPath("$[6].resources").value(10));
    }

    @Test
    @DisplayName("GET /api/job/{id} integración - trabajo existente")
    void getJobById_existente() throws Exception {
        // Construcción: no hace falta preparar nada (el trabajo 4 existe en jobs.json)
        // Prueba y verificación: GET /api/job/4 responde 200 con los datos de Backend Engineer
        mockMvc.perform(get("/api/job/4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Backend Engineer"))
                .andExpect(jsonPath("$.salary").value(9000.0))
                .andExpect(jsonPath("$.hours").value(1500))
                .andExpect(jsonPath("$.resources").value(5));
    }

    @Test
    @DisplayName("GET /api/job/{id} integración - id inexistente retorna 500 'No value present'")
    void getJobById_inexistente() throws Exception {
        // Construcción: no hace falta preparar nada (el id 3 no existe en jobs.json)
        // Prueba y verificación: GET /api/job/3 responde 500 con el mensaje "No value present"
        mockMvc.perform(get("/api/job/3"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No value present"));
    }

    @Test
    @DisplayName("GET /api/job/{id} integración - id no numérico retorna 400")
    void getJobById_idNoNumerico() throws Exception {
        // Construcción: no hace falta preparar nada (el id "abc" ya es inválido)
        // Prueba y verificación: GET /api/job/abc responde 400
        mockMvc.perform(get("/api/job/abc"))
                .andExpect(status().isBadRequest());
    }
}
