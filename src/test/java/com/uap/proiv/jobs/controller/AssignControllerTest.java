package com.uap.proiv.jobs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uap.proiv.jobs.dto.AssignRequest;
import com.uap.proiv.jobs.dto.Job;
import com.uap.proiv.jobs.dto.User;
import com.uap.proiv.jobs.dto.UserJobAssigned;
import com.uap.proiv.jobs.service.UserJobAssignedService;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitario de {@link AssignController}.
 * Contiene el caso POST /api/job/assign que antes vivía en JobControllerTest,
 * movido a /api/assign por la separación de controllers del fork.
 */
@ExtendWith(MockitoExtension.class)
class AssignControllerTest {

    @Mock
    UserJobAssignedService userJobAssignedService;

    @InjectMocks
    AssignController assignController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<User> users;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(assignController).build();

        users = new ArrayList<>();
        User user1 = new User();
        user1.setId(1);
        user1.setEmail("ejemplo@as.com");
        user1.setAvatar("null");
        user1.setFirstName("juan");
        user1.setLastName("Garcia");
        users.add(user1);

        User user2 = new User();
        user2.setId(2);
        user2.setEmail("ejemplo2@as.com");
        user2.setAvatar("null");
        user2.setFirstName("diane");
        user2.setLastName("perez");
        users.add(user2);
    }

    private String body(Integer requestNumber, String clientName) throws Exception {
        AssignRequest request = new AssignRequest();
        request.setRequestNumber(requestNumber);
        request.setClientName(clientName);
        return objectMapper.writeValueAsString(request);
    }

    @Test
    @DisplayName("POST /api/assign retorna cliente, número de pedido y asignaciones")
    void postAssign_success() throws Exception {
        // Construcción: se arman 2 trabajos y el service falso devuelve sus asignaciones (2 usuarios y 1 usuario)
        Job job1 = new Job("Developer", 5000, 2000, 1, 2);
        Job job2 = new Job("Designer", 4500, 1500, 2, 1);

        List<UserJobAssigned> asignaciones = new ArrayList<>();
        asignaciones.add(new UserJobAssigned(users, job1));
        asignaciones.add(new UserJobAssigned(List.of(users.getFirst()), job2));

        when(userJobAssignedService.assign()).thenReturn(asignaciones);

        // Prueba y verificación: POST /api/assign responde 200 con el cliente, el número de pedido y las 2 asignaciones
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(123, "Name")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Client").value("Name"))
                .andExpect(jsonPath("$.Request_Number").value(123))
                .andExpect(jsonPath("$.Assign.length()").value(2))
                .andExpect(jsonPath("$.Assign[0].job.name").value("Developer"))
                .andExpect(jsonPath("$.Assign[0].users.length()").value(2))
                .andExpect(jsonPath("$.Assign[0].users[0].first_name").value("juan"))
                .andExpect(jsonPath("$.Assign[1].job.name").value("Designer"))
                .andExpect(jsonPath("$.Assign[1].users.length()").value(1));

        // Verificación: el service se llamó una sola vez
        verify(userJobAssignedService, times(1)).assign();
    }

    @Test
    @DisplayName("POST /api/assign - Excepción del service retorna 500 con el mensaje")
    void postAssign_exception() throws Exception {
        // Construcción: el service falso lanza un error al asignar
        when(userJobAssignedService.assign()).thenThrow(new RuntimeException("Service Error"));

        // Prueba y verificación: POST /api/assign responde 500 con el mensaje "Service Error"
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(123, "Name")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Service Error"));
    }

    @Test
    @DisplayName("POST /api/assign sin clientName retorna 400 y no llama al service")
    void postAssign_sinClientName() throws Exception {
        // Construcción: no hace falta preparar nada (el body va sin clientName)
        // Prueba y verificación: POST /api/assign sin clientName responde 400
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(123, null)))
                .andExpect(status().isBadRequest());

        // Verificación: el service nunca fue llamado
        verifyNoInteractions(userJobAssignedService);
    }

    @Test
    @DisplayName("POST /api/assign con clientName vacío retorna 400")
    void postAssign_clientNameVacio() throws Exception {
        // Construcción: no hace falta preparar nada (el body va con clientName vacío)
        // Prueba y verificación: POST /api/assign con clientName vacío responde 400
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(123, "")))
                .andExpect(status().isBadRequest());

        // Verificación: el service nunca fue llamado
        verifyNoInteractions(userJobAssignedService);
    }

    @Test
    @DisplayName("POST /api/assign sin requestNumber retorna 400")
    void postAssign_sinRequestNumber() throws Exception {
        // Construcción: no hace falta preparar nada (el body va sin requestNumber)
        // Prueba y verificación: POST /api/assign sin requestNumber responde 400
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "Name")))
                .andExpect(status().isBadRequest());

        // Verificación: el service nunca fue llamado
        verifyNoInteractions(userJobAssignedService);
    }
}
