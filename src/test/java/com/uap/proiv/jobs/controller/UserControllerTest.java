package com.uap.proiv.jobs.controller;

import com.uap.proiv.jobs.dto.User;
import com.uap.proiv.jobs.dto.UserApiResponse;
import com.uap.proiv.jobs.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitario de {@link UserController}.
 * Contiene los casos GET /api/job/users/{page} que antes vivían en JobControllerTest,
 * movidos a /api/user/{page} por la separación de controllers del fork.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;

    @InjectMocks
    UserController userController;

    private MockMvc mockMvc;
    private UserApiResponse userApiResponse;
    private List<User> users;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

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

        userApiResponse = new UserApiResponse();
        userApiResponse.setPage(1);
        userApiResponse.setPerPage(2);
        userApiResponse.setTotal(2);
        userApiResponse.setTotalPages(1);
        userApiResponse.setData(users);
    }

    @Test
    @DisplayName("GET /api/user/{page} retorna usuarios")
    void getUsers_success() throws Exception {
        // Construcción: el service falso devuelve la página 1 con los 2 usuarios
        when(userService.search(1)).thenReturn(userApiResponse);

        // Prueba y verificación: GET /api/user/1 responde 200 con la página 1, 2 usuarios y el primero es juan
        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.data[0].first_name").value("juan"));
    }

    @Test
    @DisplayName("GET /api/user/{page} - Excepción retornada por el service")
    void getUsers_exception() throws Exception {
        // Construcción: el service falso lanza un error al pedir la página 2
        when(userService.search(2)).thenThrow(new RuntimeException("Service Error"));

        // Prueba y verificación: GET /api/user/2 responde 500 con el mensaje "Service Error"
        mockMvc.perform(get("/api/user/2"))
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Service Error"));
    }

    @Test
    @DisplayName("GET /api/user/id/{id} retorna el usuario")
    void getUserById_success() throws Exception {
        // Construcción: el service falso devuelve a diane cuando le piden el id 2
        when(userService.searchById(2)).thenReturn(users.get(1));

        // Prueba y verificación: GET /api/user/id/2 responde 200 con los datos de diane
        mockMvc.perform(get("/api/user/id/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.first_name").value("diane"))
                .andExpect(jsonPath("$.last_name").value("perez"));
    }

    @Test
    @DisplayName("GET /api/user/id/{id} - Excepción retornada por el service")
    void getUserById_exception() throws Exception {
        // Construcción: el service falso lanza un error al buscar el id 5
        when(userService.searchById(5)).thenThrow(new RuntimeException("Service Error"));

        // Prueba y verificación: GET /api/user/id/5 responde 500 con el mensaje "Service Error"
        mockMvc.perform(get("/api/user/id/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Service Error"));
    }

    @Test
    @DisplayName("POST /api/user/update actualiza y envía al service los datos recibidos")
    void update_success() throws Exception {
        // Construcción: se arma el JSON del usuario con id 1, morpheus / zion resident
        String json = """
                {
                  "id": 1,
                  "first_name": "morpheus",
                  "last_name": "zion resident"
                }
                """;

        // Prueba y verificación: POST /api/user/update con ese JSON responde 200 con "User created successfully"
        mockMvc.perform(post("/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));

        // Verificación: el service recibió un usuario con el id, nombre y apellido enviados
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).update(captor.capture());
        assertEquals(1, captor.getValue().getId());
        assertEquals("morpheus", captor.getValue().getFirstName());
        assertEquals("zion resident", captor.getValue().getLastName());
    }

    @Test
    @DisplayName("POST /api/user/update - Excepción retornada por el service")
    void update_exception() throws Exception {
        // Construcción: el service falso lanza un error 500 al actualizar cualquier usuario
        doThrow(new RuntimeException("Error al crear el usuario: 500")).when(userService).update(any(User.class));

        // Prueba y verificación: POST /api/user/update responde 500 con el mensaje del error
        mockMvc.perform(post("/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1, \"first_name\": \"x\", \"last_name\": \"y\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al crear el usuario: 500"));
    }
}
