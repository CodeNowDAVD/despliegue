package biblioteca.gorbits.admin;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminProvidersApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminListaProveedores_conIdYUsername() throws Exception {
        String admin = token("admin", "admin123");

        mvc.perform(get("/api/v1/admin/providers").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].username").value("proveedor"));
    }

    @Test
    void adminActualizaProveedor() throws Exception {
        String admin = token("admin", "admin123");

        long userId = objectMapper
                .readTree(mvc.perform(get("/api/v1/admin/providers").header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        mvc.perform(put("/api/v1/admin/providers/" + userId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"proveedor","firstName":"María","lastName":"Actualizada",\
                                "dni":"12345678","phone":"70000099","email":"maria.actualizada@ejemplo.com",\
                                "career":"Marketing"}\
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Actualizada"))
                .andExpect(jsonPath("$.phone").value("70000099"))
                .andExpect(jsonPath("$.career").value("Marketing"));
    }

    @Test
    void proveedorNoPuedeListarProveedores() throws Exception {
        String prov = token("proveedor", "proveedor123");

        mvc.perform(get("/api/v1/admin/providers").header("Authorization", "Bearer " + prov))
                .andExpect(status().isForbidden());
    }

    private String token(String user, String pass) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user, pass))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
