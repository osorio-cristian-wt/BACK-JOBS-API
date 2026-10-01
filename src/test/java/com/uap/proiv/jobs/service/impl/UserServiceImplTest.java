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

    // Arma 3 usuarios falsos y un UserApiResponse que imita una página de la API ReqRes:
    // page = número de página, perPage = usuarios por página, total = total de usuarios,
    // totalPages = cantidad de páginas, data = lista de usuarios de esa página.
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
            // Construcción: el repositorio falso devuelve la página 1 con los 3 usuarios
            when(userApiRepository.getUsers(1)).thenReturn(userApiResponse);

            // Prueba: se llama al servicio pidiendo la página 1
            UserApiResponse result = userService.search(1);

            // Verificación: es la misma respuesta, los jobId quedan 1, 2 y 3 y el repo se llamó una sola vez con la página 1
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
            // Construcción: setPage(2) es solo para que la respuesta falsa parezca la página 2; el repo la devuelve si le piden la 2
            userApiResponse.setPage(2);
            when(userApiRepository.getUsers(2)).thenReturn(userApiResponse);

            // Prueba: se llama al servicio pidiendo la página 2
            UserApiResponse result = userService.search(2);

            // Verificación: lo importante es el verify: el servicio le pasó al repositorio el mismo número de página (2)
            assertEquals(2, result.getPage());
            verify(userApiRepository).getUsers(2);
        }

        @Test
        @DisplayName("Éxito: página sin usuarios devuelve lista vacía sin errores")
        void search_success_paginaVacia() {
            // Construcción: el repositorio falso devuelve la página 3 sin usuarios (lista vacía)
            userApiResponse.setPage(3);
            userApiResponse.setData(new ArrayList<>());
            when(userApiRepository.getUsers(3)).thenReturn(userApiResponse);

            // Prueba: se llama al servicio con la página 3 esperando que no lance ningún error
            UserApiResponse result = assertDoesNotThrow(() -> userService.search(3));

            // Verificación: la lista de usuarios existe y está vacía
            assertNotNull(result.getData());
            assertTrue(result.getData().isEmpty());
        }

        @Test
        @DisplayName("Excepción: si el repositorio falla, la excepción se propaga sin modificarse")
        void search_exception_repositorioFalla() {
            // Construcción: el repositorio falso lanza un error de conexión con la API
            RuntimeException errorApi = new RuntimeException("Error al conectar con la API de usuarios: timeout");
            when(userApiRepository.getUsers(1)).thenThrow(errorApi);

            // Prueba: se llama al servicio con la página 1 y se captura la excepción
            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.search(1));

            // Verificación: la excepción es la misma que lanzó el repo, con el mismo mensaje, y el repo se llamó una vez
            assertSame(errorApi, ex);
            assertEquals("Error al conectar con la API de usuarios: timeout", ex.getMessage());
            verify(userApiRepository, times(1)).getUsers(1);
        }

        @Test
        @DisplayName("Excepción: respuesta sin 'data' (null) lanza NullPointerException")
        void search_exception_dataNula() {
            // Construcción: el repositorio falso devuelve una respuesta con data en null
            userApiResponse.setData(null);
            when(userApiRepository.getUsers(1)).thenReturn(userApiResponse);

            // Prueba y verificación: al llamar al servicio con la página 1 se espera un NullPointerException
            assertThrows(NullPointerException.class, () -> userService.search(1));
        }

        @Test
        @DisplayName("Excepción: el repositorio devuelve null lanza NullPointerException")
        void search_exception_respuestaNula() {
            // Construcción: el repositorio falso devuelve null en vez de una respuesta
            when(userApiRepository.getUsers(1)).thenReturn(null);

            // Prueba y verificación: al llamar al servicio con la página 1 se espera un NullPointerException
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
            // Construcción: el repositorio falso devuelve a Janet cuando le piden el id 8
            User janet = users.get(1);
            when(userApiRepository.getUserById(8)).thenReturn(janet);

            // Prueba: se llama al servicio buscando el usuario con id 8
            User result = userService.searchById(8);

            // Verificación: devuelve a Janet con sus datos sin cambios, jobId = 1 y el repo se llamó una sola vez
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
            // Construcción: el repositorio falso devuelve null para el id 999 (usuario inexistente)
            when(userApiRepository.getUserById(999)).thenReturn(null);

            // Prueba y verificación: al buscar el id 999 el servicio lanza NullPointerException
            assertThrows(NullPointerException.class, () -> userService.searchById(999));
            // Verificación: el repositorio recibió el pedido con el id 999
            verify(userApiRepository).getUserById(999);
        }

        @Test
        @DisplayName("Excepción: si el repositorio falla, la excepción se propaga")
        void searchById_exception_repositorioFalla() {
            // Construcción: el repositorio falso lanza un error 401 de la API al buscar el id 1
            when(userApiRepository.getUserById(1))
                    .thenThrow(new RuntimeException("Error en ReqRes API. Código: 401"));

            // Prueba: se llama al servicio buscando el id 1 y se captura la excepción
            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.searchById(1));

            // Verificación: la excepción llega con el mismo mensaje del repositorio
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
            // Construcción: se toma a George como usuario a actualizar (el repo falso no hace nada)
            User george = users.get(0);

            // Prueba: se llama al servicio para actualizar a George, sin que lance errores
            assertDoesNotThrow(() -> userService.update(george));

            // Verificación: el repositorio recibió a George una sola vez y nada más
            verify(userApiRepository, times(1)).updateUser(george);
            verifyNoMoreInteractions(userApiRepository);
        }

        @Test
        @DisplayName("Excepción: error del repositorio se envuelve en RuntimeException con mensaje propio y causa original")
        void update_exception_repositorioFalla() {
            // Construcción: el repositorio falso lanza un error 500 al actualizar a George
            User george = users.get(0);
            RuntimeException causa = new RuntimeException("Error en ReqRes API. Código: 500");
            doThrow(causa).when(userApiRepository).updateUser(george);

            // Prueba: se llama al servicio para actualizar a George y se captura la excepción
            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.update(george));

            // Verificación: el mensaje es el propio del servicio, la causa es el error original y el repo se llamó una vez
            assertEquals("Error al crear el usuario: Error en ReqRes API. Código: 500", ex.getMessage());
            assertSame(causa, ex.getCause());
            verify(userApiRepository, times(1)).updateUser(george);
        }

        @Test
        @DisplayName("Excepción: usuario null -> el error del repositorio también se envuelve")
        void update_exception_usuarioNulo() {
            // Construcción: el repositorio falso lanza NullPointerException si recibe un usuario null
            doThrow(new NullPointerException("user es null")).when(userApiRepository).updateUser(null);

            // Prueba: se llama al servicio para actualizar un usuario null y se captura la excepción
            RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.update(null));

            // Verificación: el error viene envuelto con el mensaje del servicio y la causa es un NullPointerException
            assertEquals("Error al crear el usuario: user es null", ex.getMessage());
            assertTrue(ex.getCause() instanceof NullPointerException);
        }
    }
}
