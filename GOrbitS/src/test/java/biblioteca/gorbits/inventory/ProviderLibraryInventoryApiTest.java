package biblioteca.gorbits.inventory;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class ProviderLibraryInventoryApiTest {

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
        resetWarehouseTo500PerBook();
    }

    private void resetWarehouseTo500PerBook() throws Exception {
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
    void proveedorRegistraFacturaLibreriaYDevolucionStockYConciliacion() throws Exception {
        String prov = token("proveedor", "proveedor123");

        JsonNode firstBook = firstUnitarioBook(prov);
        long bookId = firstBook.get("id").asLong();
        double facturaImporte = firstBook.get("price").asDouble() * 10;

        long campaignId =
                objectMapper.readTree(mvc.perform(get("/api/v1/reference/campaigns")
                                        .header("Authorization", "Bearer " + prov))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                        .get(0)
                        .get("id")
                        .asLong();

        JsonNode summaryAntes = reconciliationSummary(mvc, prov, campaignId);
        double invoicedAntes = summaryAntes.get("totalInvoicedAmount").asDouble();
        double depositsAntes = summaryAntes.get("totalDepositsToLibrary").asDouble();
        int returnedAntes = summaryAntes.get("returnedToLibraryUnits").asInt();
        int disponibleAntes = disponibleForBook(mvc, prov, bookId);

        String fac =
                """
                {"invoiceNumber":"FAC-PROV-UNO","issuedOn":"2026-02-01","note":null,"lines":[{"bookId":%d,"quantity":10}]}
                """
                        .formatted(bookId);

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fac))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUsername").value("proveedor"));
        int disponibleTrasFactura = disponibleForBook(mvc, prov, bookId);
        assertEquals(disponibleAntes + 10, disponibleTrasFactura);

        String ret =
                """
                {"campaignId":%d,"note":"sobrante","lines":[{"bookId":%d,"quantity":4}]}
                """
                        .formatted(campaignId, bookId);

        long returnId = objectMapper
                .readTree(mvc.perform(post("/api/v1/inventory/library-stock-returns")
                                .header("Authorization", "Bearer " + prov)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ret))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.totalUnits").value(4))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();

        mvc.perform(get("/api/v1/inventory/library-stock-returns").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + returnId + ")]").exists());

        mvc.perform(get("/api/v1/inventory/library-stock-returns/" + returnId)
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnId))
                .andExpect(jsonPath("$.totalUnits").value(4));

        assertEquals(disponibleTrasFactura - 4, disponibleForBook(mvc, prov, bookId));

        mvc.perform(get("/api/v1/inventory/library-reconciliation/summary").param("campaignId", String.valueOf(campaignId))
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andExpect(jsonPath("$.purchasedFromLibraryUnits").exists())
                .andExpect(jsonPath("$.returnedToLibraryUnits").value(returnedAntes + 4))
                .andExpect(jsonPath("$.netPurchasedUnits").exists())
                .andExpect(jsonPath("$.unitsInClosedGuides").exists())
                .andExpect(jsonPath("$.totalInvoicedAmount").value(invoicedAntes + facturaImporte))
                .andExpect(jsonPath("$.totalDepositsToLibrary").value(depositsAntes))
                .andExpect(jsonPath("$.netBalanceOwedToLibrary").value(invoicedAntes + facturaImporte - depositsAntes));

        String pago =
                """
                {"amount":100.00,"paidOn":"2026-03-01","note":"abono","campaignId":%d}
                """
                        .formatted(campaignId);
        mvc.perform(post("/api/v1/inventory/library-payments")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pago))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/inventory/library-reconciliation/summary").param("campaignId", String.valueOf(campaignId))
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDepositsToLibrary").value(depositsAntes + 100))
                .andExpect(jsonPath("$.netBalanceOwedToLibrary").value(invoicedAntes + facturaImporte - depositsAntes - 100));
    }

    private JsonNode firstUnitarioBook(String jwt) throws Exception {
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + jwt))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        for (JsonNode b : arr) {
            if ("UNITARIO".equals(b.get("bookType").asText())) {
                return b;
            }
        }
        throw new AssertionError("Sin libro UNITARIO en catálogo");
    }

    private JsonNode reconciliationSummary(MockMvc mvc, String jwt, long campaignId) throws Exception {
        return objectMapper.readTree(mvc.perform(get("/api/v1/inventory/library-reconciliation/summary")
                                .param("campaignId", String.valueOf(campaignId))
                                .header("Authorization", "Bearer " + jwt))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    private int disponibleForBook(MockMvc mvc, String jwt, long bookId) throws Exception {
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/inventory/my-stock")
                                .header("Authorization", "Bearer " + jwt))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        for (int i = 0; i < arr.size(); i++) {
            JsonNode row = arr.get(i);
            if (row.get("bookId").asLong() == bookId) {
                return row.get("available").asInt();
            }
        }
        return 0;
    }

    private int quantityWarehouseForBook(MockMvc mvc, String jwt, long bookId) throws Exception {
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/inventory/warehouse")
                                .header("Authorization", "Bearer " + jwt))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        for (int i = 0; i < arr.size(); i++) {
            JsonNode row = arr.get(i);
            if (row.get("bookId").asLong() == bookId) {
                return row.get("quantity").asInt();
            }
        }
        throw new AssertionError("book not in warehouse list");
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
