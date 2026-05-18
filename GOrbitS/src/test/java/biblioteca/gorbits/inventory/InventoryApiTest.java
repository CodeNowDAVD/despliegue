package biblioteca.gorbits.inventory;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import biblioteca.gorbits.inventory.dto.SetWarehouseStockRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
class InventoryApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    WebApplicationContext context;

    @Autowired
    ProviderFieldStockRepository providerFieldStockRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        providerFieldStockRepository.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        resetWarehouseStockBase();
    }

    /** Aísla de otros tests que comparten H2 y modifican almacén/campo. */
    private void resetWarehouseStockBase() throws Exception {
        String admin = token("admin", "admin123");
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        for (JsonNode b : arr) {
            long bookId = b.get("id").asLong();
            mvc.perform(put("/api/v1/inventory/warehouse/books/" + bookId)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":500}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(1)
    void adminYProveedorListanAlmacenSoloAdminAjustaStock() throws Exception {
        String admin = token("admin", "admin123");
        String prov = token("proveedor", "proveedor123");

        mvc.perform(get("/api/v1/inventory/warehouse").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(500));

        mvc.perform(get("/api/v1/inventory/warehouse").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(500));

        mvc.perform(put("/api/v1/inventory/warehouse/books/999999")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    void facturaLibreriaAumentaMiStockProveedor() throws Exception {
        String prov = token("proveedor", "proveedor123");
        long bookId = firstUnitarioBookId(prov);

        int antes = disponible(prov, bookId);

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"invoiceNumber\":\"FAC-INV-API\",\"issuedOn\":\"2026-03-01\",\"note\":null,\"lines\":[{\"bookId\":"
                                        + bookId + ",\"quantity\":3}]}"))
                .andExpect(status().isCreated());

        assertEquals(antes + 3, disponible(prov, bookId));
    }

    private String token(String user, String pass) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user, pass))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode n = objectMapper.readTree(r.getResponse().getContentAsString());
        return n.get("accessToken").asText();
    }

    private long firstUnitarioBookId(String jwt) throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/catalog/books").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = objectMapper.readTree(r.getResponse().getContentAsString());
        for (JsonNode b : arr) {
            if ("UNITARIO".equals(b.get("bookType").asText())) {
                return b.get("id").asLong();
            }
        }
        throw new AssertionError("Sin libro UNITARIO en catálogo");
    }

    private int disponible(String jwt, long bookId) throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/inventory/my-stock").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = objectMapper.readTree(r.getResponse().getContentAsString());
        for (JsonNode n : arr) {
            if (n.get("bookId").asLong() == bookId) {
                return n.get("available").asInt();
            }
        }
        return 0;
    }
}
