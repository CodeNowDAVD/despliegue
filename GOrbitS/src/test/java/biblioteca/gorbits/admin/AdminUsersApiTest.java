package biblioteca.gorbits.admin;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminUsersApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @Order(1)
    void adminListaUsuariosYRedefineContraseñaProveedor() throws Exception {
        String admin = token("admin", "admin123");

        long proveedorId = findProveedorId(admin);

        mvc.perform(put("/api/v1/admin/users/" + proveedorId + "/password")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"nuevaClave9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("proveedor"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(new LoginRequest("proveedor", "proveedor123"))))
                .andExpect(status().isUnauthorized());

        token("proveedor", "nuevaClave9");

        mvc.perform(put("/api/v1/admin/users/" + proveedorId + "/password")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"proveedor123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void proveedorNoGestionaUsuarios() throws Exception {
        String prov = token("proveedor", "proveedor123");

        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + prov))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/admin/users/providers")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"xx","password":"abcdefgh","firstName":"X","lastName":"Y",\
                                "dni":"11111111","phone":"1","email":"x@y.com","career":"Z"}\
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void adminNoPuedeDeshabilitarse() throws Exception {
        String admin = token("admin", "admin123");
        long adminId = objectMapper.readTree(mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();

        mvc.perform(patch("/api/v1/admin/users/" + adminId + "/enabled")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void adminDeshabilitaYHabilitaProveedor() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = findProveedorId(admin);

        mvc.perform(patch("/api/v1/admin/users/" + proveedorId + "/enabled")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("proveedor", "proveedor123"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/v1/admin/users/" + proveedorId + "/enabled")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        token("proveedor", "proveedor123");
    }

    @Test
    @Order(5)
    void adminCambiaRolProveedorYVuelve() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = findProveedorId(admin);

        mvc.perform(patch("/api/v1/admin/users/" + proveedorId + "/role")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(patch("/api/v1/admin/users/" + proveedorId + "/role")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PROVEEDOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROVEEDOR"));
    }

    @Test
    @Order(6)
    void adminCreaProveedor() throws Exception {
        String admin = token("admin", "admin123");

        mvc.perform(post("/api/v1/admin/users/providers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"proveedor2","password":"claveProvee2","firstName":"Ana",\
                                "lastName":"López","dni":"87654321","phone":"70000002",\
                                "email":"ana.lopez@ejemplo.com","career":"Contaduría"}\
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("proveedor2"))
                .andExpect(jsonPath("$.role").value("PROVEEDOR"));

        String prov2 = token("proveedor2", "claveProvee2");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/v1/me/provider-profile")
                        .header("Authorization", "Bearer " + prov2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("López"))
                .andExpect(jsonPath("$.dni").value("87654321"));
    }

    private long findProveedorId(String adminJwt) throws Exception {
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        for (JsonNode u : arr) {
            if ("proveedor".equals(u.get("username").asText())) {
                return u.get("id").asLong();
            }
        }
        throw new AssertionError("seed proveedor");
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
