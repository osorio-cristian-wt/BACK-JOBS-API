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
        mockMvc.perform(get("/api/job/3"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No value present"));
    }

    @Test
    @DisplayName("GET /api/job/{id} integración - id no numérico retorna 400")
    void getJobById_idNoNumerico() throws Exception {
        mockMvc.perform(get("/api/job/abc"))
                .andExpect(status().isBadRequest());
    }
}
