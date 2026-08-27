import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.junit.jupiter.api.BeforeAll;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import java.net.http.HttpClient;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserApiRepository userApiRepository;

    static MockWebServer mockWebServer;

    @BeforeAll
    public static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @TestConfiguration
    static class TestConfig {
    
        @Bean
        @Primary
        UserApiRepository userApiRepository(ObjectMapper objectMapper) {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            String baseUrl = mockWebServer.url("/api/users").toString();
            String apiKey = "test-api-key";
            return new UserApiRepository(httpClient, baseUrl, apiKey, objectMapper);
        }
    }

    @Test
    @DisplayName("GET api/users/{id} integracion UserController, UserServices, UserApiRepository")
    void getUserById() throws Exception {
        String jsonResponse = """
        {
            "id": 1,
            "email": "john.doe@example.com"
            "firstName": "John",
            "lastName": "Doe",
            "avatar": "https://example.com/avatar.jpg"
        }
        """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(jsonResponse)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            
        );
            
        mockMvc.perform(get("/api/user/id/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.avatar").value("https://example.com/avatar.jpg")
            );

        RecordRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("application/json", recordedRequest.getHeader("Accept"));
        assertEquals("test-api-key", recordedRequest.getHeader("X-API-KEY"));
    }


     @Test
    @DisplayName("POST api/users/{id} integracion UserController, UserServices, UserApiRepository")
    void updateUser_success() throws Exception {
        String updateResponse = """
        {
            "name": "Peter",
            "job": "Paker",
            "updatedAt": "2024-06-05T12:00:00Z"
        }
        """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(updateResponse)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            
        );
            
        String userJson = """
        {
            "id": 1,
            "firstName": "Peter",
            "lastName": "Parker",
        }
        """;
        mockMvc.perform(post("/api/user/update"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Peter"))
            .andExpect(jsonPath("$.job").value("Paker"))
            .andExpect(jsonPath("$.updatedAt").value("2024-06-05T12:00:00Z"))
            ;

        RecordRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/api/user/update", recordedRequest.getPath());
        assertEquals("application/json", recordedRequest.getHeader("Accept"));
        assertEquals("test-api-key", recordedRequest.getHeader("X-API-KEY"));
    }
    
}