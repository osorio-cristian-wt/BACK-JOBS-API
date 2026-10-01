package com.uap.proiv.jobs.controller;

import com.uap.proiv.jobs.dto.Job;
import com.uap.proiv.jobs.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitario de {@link JobController} (corregido por la actualización del fork).
 *
 * Con la "Separación de Controllers" el JobController quedó solo con:
 *   GET /api/job/all   y   GET /api/job/{id}
 * Los casos de /api/job/users/{page} pasaron a {@link UserControllerTest} (/api/user/{page})
 * y los de /api/job/assign a {@link AssignControllerTest} (/api/assign).
 * Por eso el controller ahora solo depende de JobService.
 */
@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    JobService jobService;

    @InjectMocks
    JobController jobController;

    private MockMvc mockMvc;
    private List<Job> jobs;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobController).build();

        jobs = new ArrayList<>();
        jobs.add(new Job("Developer", 5000, 2000, 1, 3));
        jobs.add(new Job("Designer", 4500, 1500, 2, 1));
    }

    @Test
    @DisplayName("GET /api/job/all retorna todos los trabajos")
    void getAllJobs_success() throws Exception {
        when(jobService.getAllJobs()).thenReturn(jobs);

        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Developer"))
                .andExpect(jsonPath("$[0].salary").value(5000.0))
                .andExpect(jsonPath("$[0].hours").value(2000))
                .andExpect(jsonPath("$[0].resources").value(3))
                .andExpect(jsonPath("$[1].name").value("Designer"));

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    @DisplayName("GET /api/job/all sin trabajos retorna lista vacía")
    void getAllJobs_listaVacia() throws Exception {
        when(jobService.getAllJobs()).thenReturn(List.of());

        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/job/all - Excepción del service retorna 500 con el mensaje")
    void getAllJobs_exception() throws Exception {
        when(jobService.getAllJobs()).thenThrow(new RuntimeException("Error al leer jobs.json"));

        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al leer jobs.json"));
    }

    @Test
    @DisplayName("GET /api/job/{id} retorna el trabajo solicitado")
    void getJobById_success() throws Exception {
        when(jobService.getJobById(2)).thenReturn(jobs.get(1));

        mockMvc.perform(get("/api/job/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Designer"))
                .andExpect(jsonPath("$.salary").value(4500.0))
                .andExpect(jsonPath("$.hours").value(1500))
                .andExpect(jsonPath("$.resources").value(1));

        verify(jobService, times(1)).getJobById(2);
    }

    @Test
    @DisplayName("GET /api/job/{id} inexistente - el service lanza NoSuchElementException y retorna 500")
    void getJobById_noExiste() throws Exception {
        when(jobService.getJobById(99)).thenThrow(new NoSuchElementException("No value present"));

        mockMvc.perform(get("/api/job/99"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No value present"));
    }

    @Test
    @DisplayName("GET /api/job/{id} con id no numérico retorna 400 y no llama al service")
    void getJobById_idInvalido() throws Exception {
        mockMvc.perform(get("/api/job/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobService);
    }
}
