package biblioteca.gorbits.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.catalog.dto.BookRequest;
import biblioteca.gorbits.catalog.dto.CategoryRequest;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.CreateSalesContractTagRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import biblioteca.gorbits.commercial.dto.UpdateProviderProfileRequest;
import biblioteca.gorbits.testsupport.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Cubre endpoints y ramas no alcanzados por otros *ApiTest (sube cobertura global hacia 100%).
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiGapCoverageTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void catalogAdminCrudCompleto() throws Exception {
        String admin = ApiTestSupport.login(mvc, "admin", "admin123");

        JsonNode createdCat = ApiTestSupport.JSON.readTree(mvc.perform(post("/api/v1/catalog/categories")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ApiTestSupport.JSON.writeValueAsString(new CategoryRequest("Cat API Gap"))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long catId = createdCat.get("id").asLong();

        mvc.perform(get("/api/v1/catalog/categories/" + catId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cat API Gap"));

        mvc.perform(put("/api/v1/catalog/categories/" + catId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(new CategoryRequest("Cat API Gap Edit"))))
                .andExpect(status().isOk());

        var bookReq = new BookRequest(catId, "Libro Gap", new BigDecimal("15.00"), BookType.UNITARIO, null, null, null);
        JsonNode book = ApiTestSupport.JSON.readTree(mvc.perform(post("/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ApiTestSupport.JSON.writeValueAsString(bookReq)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long bookId = book.get("id").asLong();

        mvc.perform(get("/api/v1/catalog/books/" + bookId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/catalog/books/" + bookId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(
                                new BookRequest(catId, "Libro Gap Edit", new BigDecimal("16.00"), BookType.UNITARIO, null, null, null))))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/catalog/books/" + bookId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/v1/catalog/categories/" + catId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void proveedorEndpointsAdicionales() throws Exception {
        String prov = ApiTestSupport.login(mvc, "proveedor", "proveedor123");
        long bookId = ApiTestSupport.firstUnitarioBookId(mvc, prov);
        ApiTestSupport.facturaLibreria(mvc, prov, bookId, 30, "FAC-GAP-01");

        JsonNode zones = ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/reference/zones")
                                .header("Authorization", "Bearer " + prov))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long zoneId = ApiTestSupport.firstId(zones);

        mvc.perform(get("/api/v1/me/provider-profile").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/me/provider-profile")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(new UpdateProviderProfileRequest(zoneId))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/sales-contract-tags")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(new CreateSalesContractTagRequest("VIP-Gap"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/sales-contract-tags").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/inventory/my-stock").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/inventory/movements").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/inventory/stock/by-category").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        JsonNode camps = ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/reference/campaigns")
                                .header("Authorization", "Bearer " + prov))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long campId = ApiTestSupport.firstId(camps);

        mvc.perform(get("/api/v1/provider/campaigns/" + campId + "/close-report")
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value((int) campId));

        mvc.perform(post("/api/v1/inventory/library-payments")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"amount":50.00,"paidOn":"2026-03-10","note":"gap","campaignId":%d}
                                """
                                        .formatted(campId)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/inventory/library-payments").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());
    }

    @Test
    void billingCalendarioYErroresHandler() throws Exception {
        String prov = ApiTestSupport.login(mvc, "proveedor", "proveedor123");

        mvc.perform(get("/api/v1/billing/calendar")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/catalog/books/999999").header("Authorization", "Bearer " + prov))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));

        mvc.perform(post("/api/v1/admin/users/providers")
                        .header("Authorization", "Bearer " + ApiTestSupport.login(mvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void guiaConEtiquetaYCalendario() throws Exception {
        String prov = ApiTestSupport.login(mvc, "proveedor", "proveedor123");
        long bookId = ApiTestSupport.firstUnitarioBookId(mvc, prov);
        ApiTestSupport.facturaLibreria(mvc, prov, bookId, 20, "FAC-GAP-02");

        long campId = ApiTestSupport.firstId(ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/reference/campaigns")
                        .header("Authorization", "Bearer " + prov))
                .andReturn()
                .getResponse()
                .getContentAsString()));

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(new ClientRequest("Gap Client", "1", null, null))))
                .andExpect(status().isCreated());
        long clientId = ApiTestSupport.firstId(ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/clients")
                        .header("Authorization", "Bearer " + prov))
                .andReturn()
                .getResponse()
                .getContentAsString()));

        long tagId = ApiTestSupport.firstId(ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/sales-contract-tags")
                        .header("Authorization", "Bearer " + prov))
                .andReturn()
                .getResponse()
                .getContentAsString()));

        var guide = new CreateGuideRequest(
                campId,
                clientId,
                "000777",
                LocalDate.of(2026, 2, 1),
                null,
                null,
                List.of(tagId),
                List.of(new GuideLineRequest(bookId, 1, new BigDecimal("10.00"))));
        mvc.perform(post("/api/v1/guides")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(guide)))
                .andExpect(status().isCreated());
    }

    @Test
    void clienteCrudYResumenCobranza() throws Exception {
        String prov = ApiTestSupport.login(mvc, "proveedor", "proveedor123");

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(new ClientRequest("Cliente Gap CRUD", "555", null, "dir"))))
                .andExpect(status().isCreated());

        long clientId = ApiTestSupport.firstId(ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/clients")
                                .param("q", "Gap CRUD")
                                .header("Authorization", "Bearer " + prov))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()));

        mvc.perform(get("/api/v1/clients/" + clientId).header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/clients/" + clientId)
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiTestSupport.JSON.writeValueAsString(new ClientRequest("Cliente Gap Edit", "556", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Cliente Gap Edit"));

        mvc.perform(get("/api/v1/clients/" + clientId + "/billing-summary")
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/clients/" + clientId).header("Authorization", "Bearer " + prov))
                .andExpect(status().isNoContent());
    }

    @Test
    void pagoLibreriaDetalle() throws Exception {
        String prov = ApiTestSupport.login(mvc, "proveedor", "proveedor123");
        long campId = ApiTestSupport.firstId(ApiTestSupport.JSON.readTree(mvc.perform(get("/api/v1/reference/campaigns")
                        .header("Authorization", "Bearer " + prov))
                .andReturn()
                .getResponse()
                .getContentAsString()));

        JsonNode created = ApiTestSupport.JSON.readTree(mvc.perform(post("/api/v1/inventory/library-payments")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"amount":25.00,"paidOn":"2026-03-20","note":"gap-detail","campaignId":%d}
                                """
                                        .formatted(campId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        mvc.perform(get("/api/v1/inventory/library-payments/" + created.get("id").asLong())
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(25.0));
    }
}
