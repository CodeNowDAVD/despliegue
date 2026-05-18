package biblioteca.gorbits.admin;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceRepository;
import biblioteca.gorbits.inventory.ProviderFieldStockRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
class AdminDashboardApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    WebApplicationContext context;

    @Autowired
    ProviderFieldStockRepository providerFieldStockRepository;

    @Autowired
    LibrarySupplyInvoiceRepository librarySupplyInvoiceRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminVeResumenYproveedorNo() throws Exception {
        String admin = token("admin", "admin123");
        String prov = token("proveedor", "proveedor123");

        long expectedWarehouse = sumWarehouseQuantities(admin);
        long expectedField = providerFieldStockRepository.sumTotalQuantity();
        long expectedLibrary = librarySupplyInvoiceRepository.sumAllPurchasedUnits();

        mvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guidesByStatus.activa").exists())
                .andExpect(jsonPath("$.guidesByStatus.cerrada").exists())
                .andExpect(jsonPath("$.guidesByStatus.devuelta").exists())
                .andExpect(jsonPath("$.totalWarehouseUnits").value(expectedWarehouse))
                .andExpect(jsonPath("$.totalFieldStockUnits").value(expectedField))
                .andExpect(jsonPath("$.totalLibraryPurchasedUnits").value(expectedLibrary))
                .andExpect(jsonPath("$.topBooksClosedGuides").isArray());

        mvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + prov))
                .andExpect(status().isForbidden());
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

    private long sumWarehouseQuantities(String adminJwt) throws Exception {
        String body = mvc.perform(get("/api/v1/inventory/warehouse").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long sum = 0;
        for (JsonNode row : objectMapper.readTree(body)) {
            sum += row.get("quantity").asInt();
        }
        return sum;
    }
}
