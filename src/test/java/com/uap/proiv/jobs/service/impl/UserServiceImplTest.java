package com.uap.proiv.jobs.service.impl;

import com.uap.proiv.jobs.client.UserApiRepository;
import com.uap.proiv.jobs.dto.User;
import com.uap.proiv.jobs.dto.UserApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link UserServiceImpl}.
 * El repositorio (cliente de la API externa ReqRes) se reemplaza por un mock de Mockito,
 * por lo que solo se prueba la lógica del servicio.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserApiRepository userApiRepository;

    @InjectMocks
    UserServiceImpl userService;

    private List<User> users;
    private UserApiResponse userApiResponse;

    @BeforeEach
    void setup() {
        users = new ArrayList<>();
        users.add(crearUsuario(7, "george.bluth@reqres.in", "George", "Bluth"));
        users.add(crearUsuario(8, "janet.weaver@reqres.in", "Janet", "Weaver"));
        users.add(crearUsuario(9, "emma.wong@reqres.in", "Emma", "Wong"));

        userApiResponse = new UserApiResponse();
        userApiResponse.setPage(1);
        userApiResponse.setPerPage(6);
        userApiResponse.setTotal(3);
        userApiResponse.setTotalPages(1);
        userApiResponse.setData(users);
    }

    private static User crearUsuario(int id, String email, String nombre, String apellido) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(nombre);
        user.setLastName(apellido);
        user.setAvatar("https://reqres.in/img/faces/" + id + "-image.jpg");
        return user;
    }

    // ------------------------------------------------------------------ search(page)

    @Nested
    @DisplayName("search(page)")
    class Search {

        @Test
        @DisplayName("Éxito: devuelve la respuesta del repositorio y asigna jobId correlativos (1..n)")
        void search_success_asignaJobIdsCorrelativos() {
            when(userApiRepository.getUsers(1)).thenReturn(userApiResponse);

            UserApiResponse result = userService.search(1);

            assertSame(userApiResponse, result);
            assertEquals(3, result.getData().size());
            assertEquals(1, result.getData().get(0).getJobId());
            assertEquals(2, result.getData().get(1).getJobId());
            assertEquals(3, result.getData().get(2).getJobId());
            // los datos del usuario no se modifican
            assertEquals("George", result.getData().get(0).getFirstName());
            assertEquals(7, result.getData().get(0).getId());

            verify(userApiRepository, times(1)).getUsers(1);
            verifyNoMoreInteractions(userApiRepository);
        }

        @Test
        @DisplayName("Éxito: pasa al repositorio el número de página recibido")
        void search_success_usaLaPaginaRecibida() {
            userApiResponse.setPage(2);
            when(userApiRepository.getUsers(2)).thenReturn(userApiResponse);

            UserApiResponse result = userService.search(2);

            assertEquals(2, result.getPage());
            verify(userApiRepository).getUsers(2);
        }

        @Test
        @DisplayName("Éxito: página sin usuarios devuelve lista vacía sin errores")
        void search_success_paginaVacia() {
            userApiResponse.setPage(3);
            userApiResponse.setData(new ArrayList<>());
            when(userApiRepository.getUsers(3)).thenReturn(userApiResponse);

            UserApiResponse result = assertDoesNotThrow(() -> userService.search(3));

            assertNotNull(result.getData());
            assertTrue(result.getData().isEmpty());
        }

        @Test
        @DisplayName("Excepción: si el repositorio falla, la excepción se propaga sin modificarse")
        void search_exception_repositorioFalla() {
            RuntimeException errorApi = new RuntimeException("Error al conectar con la API de usuarios: timeout");
            when(userApiRepository.getUsers(1)).thenThrow(errorApi);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.search(1));

            assertSame(errorApi, ex);
            assertEquals("Error al conectar con la API de usuarios: timeout", ex.getMessage());
            verify(userApiRepository, times(1)).getUsers(1);
        }

        @Test
        @DisplayName("Excepción: respuesta sin 'data' (null) lanza NullPointerException")
        void search_exception_dataNula() {
            userApiResponse.setData(null);
            when(userApiRepository.getUsers(1)).thenReturn(userApiResponse);

            assertThrows(NullPointerException.class, () -> userService.search(1));
        }

        @Test
        @DisplayName("Excepción: el repositorio devuelve null lanza NullPointerException")
        void search_exception_respuestaNula() {
            when(userApiRepository.getUsers(1)).thenReturn(null);

            assertThrows(NullPointerException.class, () -> userService.search(1));
        }
    }

    // ------------------------------------------------------------------ searchById(id)

    @Nested
    @DisplayName("searchById(id)")
    class SearchById {

        @Test
        @DisplayName("Éxito: devuelve el usuario encontrado con jobId = 1")
        void searchById_success() {
            User janet = users.get(1);
            when(userApiRepository.getUserById(8)).thenReturn(janet);

            User result = userService.searchById(8);

            assertSame(janet, result);
            assertEquals(8, result.getId());
            assertEquals("Janet", result.getFirstName());
            assertEquals("Weaver", result.getLastName());
            assertEquals("janet.weaver@reqres.in", result.getEmail());
            assertEquals(1, result.getJobId());

            verify(userApiRepository, times(1)).getUserById(8);
            verifyNoMoreInteractions(userApiRepository);
        }

        @Test
        @DisplayName("Excepción: usuario inexistente (repositorio devuelve null) lanza NullPointerException")
        void searchById_exception_usuarioInexistente() {
            when(userApiRepository.getUserById(999)).thenReturn(null);

            assertThrows(NullPointerException.class, () -> userService.searchById(999));
            verify(userApiRepository).getUserById(999);
        }

        @Test
        @DisplayName("Excepción: si el repositorio falla, la excepción se propaga")
        void searchById_exception_repositorioFalla() {
            when(userApiRepository.getUserById(1))
                    .thenThrow(new RuntimeException("Error en ReqRes API. Código: 401"));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.searchById(1));

            assertEquals("Error en ReqRes API. Código: 401", ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ update(user)

    @Nested
    @DisplayName("update(user)")
    class Update {

        @Test
        @DisplayName("Éxito: delega la actualización en el repositorio una sola vez")
        void update_success() {
            User george = users.get(0);

            assertDoesNotThrow(() -> userService.update(george));

            verify(userApiRepository, times(1)).updateUser(george);
            verifyNoMoreInteractions(userApiRepository);
        }

        @Test
        @DisplayName("Excepción: error del repositorio se envuelve en RuntimeException con mensaje propio y causa original")
        void update_exception_repositorioFalla() {
            User george = users.get(0);
            RuntimeException causa = new RuntimeException("Error en ReqRes API. Código: 500");
            doThrow(causa).when(userApiRepository).updateUser(george);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.update(george));

            assertEquals("Error al crear el usuario: Error en ReqRes API. Código: 500", ex.getMessage());
            assertSame(causa, ex.getCause());
            verify(userApiRepository, times(1)).updateUser(george);
        }

        @Test
        @DisplayName("Excepción: usuario null -> el error del repositorio también se envuelve")
        void update_exception_usuarioNulo() {
            doThrow(new NullPointerException("user es null")).when(userApiRepository).updateUser(null);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.update(null));

            assertEquals("Error al crear el usuario: user es null", ex.getMessage());
            assertTrue(ex.getCause() instanceof NullPointerException);
        }
    }
}
