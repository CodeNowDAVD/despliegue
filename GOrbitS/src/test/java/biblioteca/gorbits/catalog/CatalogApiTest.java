package biblioteca.gorbits.catalog;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import biblioteca.gorbits.catalog.dto.BookRequest;
import biblioteca.gorbits.catalog.dto.CategoryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class CatalogApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void proveedorPuedeListarCategorias() throws Exception {
        String jwt = tokenProveedor();

        mvc.perform(get("/api/v1/catalog/categories").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void proveedorNoPuedeCrearCategoria() throws Exception {
        String jwt = tokenProveedor();
        String body = objectMapper.writeValueAsString(new CategoryRequest("Nueva categoría"));

        mvc.perform(post("/api/v1/catalog/categories")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearYListarLibros() throws Exception {
        String jwt = tokenAdmin();

        mvc.perform(get("/api/v1/catalog/categories").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        String catJson = mvc.perform(get("/api/v1/catalog/categories").header("Authorization", "Bearer " + jwt))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long firstCategoryId = objectMapper.readTree(catJson).get(0).get("id").asLong();

        var bookReq =
                new BookRequest(
                        firstCategoryId,
                        "Libro de prueba admin",
                        new BigDecimal("9.99"),
                        BookType.UNITARIO,
                        null,
                        null,
                        null);
        mvc.perform(post("/api/v1/catalog/books")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Libro de prueba admin"));
    }

    private String tokenProveedor() throws Exception {
        return token(new LoginRequest("proveedor", "proveedor123"));
    }

    private String tokenAdmin() throws Exception {
        return token(new LoginRequest("admin", "admin123"));
    }

    private String token(LoginRequest login) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
