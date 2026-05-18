package biblioteca.gorbits.billing;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class BillingApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void planCuotasCalendarioYPago() throws Exception {
        String j = token("proveedor", "proveedor123");

        long campId = firstId(mvc.perform(get("/api/v1/reference/campaigns").header("Authorization", "Bearer " + j)));
        long bookId = firstUnitarioBookId(j);

        facturaParaStock(j, bookId, 50, "FAC-BIL-PLAN");

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new biblioteca.gorbits.commercial.dto.ClientRequest("Ana", "111", null, null))))
                .andExpect(status().isCreated());
        long clientId = firstId(mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + j)));

        var guideReq = new CreateGuideRequest(
                campId,
                clientId,
                "000301",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(new GuideLineRequest(bookId, 1, new BigDecimal("20.00"))));

        MvcResult gr = mvc.perform(post("/api/v1/guides")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guideReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long guideId = objectMapper.readTree(gr.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/guides/" + guideId + "/installments/plan/custom")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"items\":[{\"dueDate\":\"2026-04-01\",\"amount\":12},{\"dueDate\":\"2026-05-01\",\"amount\":8}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/v1/billing/calendar")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pendingCount").value(1));

        long instId =
                objectMapper.readTree(mvc.perform(get("/api/v1/guides/" + guideId + "/installments")
                                        .header("Authorization", "Bearer " + j))
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                        .get(0)
                        .get("id")
                        .asLong();

        mvc.perform(post("/api/v1/installments/" + instId + "/payments")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":12.00,\"paidOn\":\"2026-04-01\",\"note\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installment.fullyPaid").value(true))
                .andExpect(jsonPath("$.clientMessage").isString());

        mvc.perform(get("/api/v1/billing/calendar")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void planEqualNoDisponible() throws Exception {
        String j = token("proveedor", "proveedor123");
        long campId = firstId(mvc.perform(get("/api/v1/reference/campaigns").header("Authorization", "Bearer " + j)));
        long bookId = firstUnitarioBookId(j);

        facturaParaStock(j, bookId, 50, "FAC-BIL-EQ");

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new biblioteca.gorbits.commercial.dto.ClientRequest("Luis", "222", null, null))))
                .andExpect(status().isCreated());
        long clientId = firstId(mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + j)));

        var guideReq = new CreateGuideRequest(
                campId,
                clientId,
                "000302",
                LocalDate.of(2026, 3, 2),
                null,
                null,
                null,
                List.of(new GuideLineRequest(bookId, 1, new BigDecimal("30.00"))));

        MvcResult gr = mvc.perform(post("/api/v1/guides")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guideReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long guideId = objectMapper.readTree(gr.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/guides/" + guideId + "/installments/plan/equal")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installmentCount\":2,\"firstDueDate\":\"2026-06-01\",\"stepMonths\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagoBloqueadoSiContratoDevuelto() throws Exception {
        String j = token("proveedor", "proveedor123");
        long campId = firstId(mvc.perform(get("/api/v1/reference/campaigns").header("Authorization", "Bearer " + j)));
        long bookId = firstUnitarioBookId(j);
        facturaParaStock(j, bookId, 50, "FAC-BIL-RET-PAY");

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new biblioteca.gorbits.commercial.dto.ClientRequest("Ret Pago", "444", null, null))))
                .andExpect(status().isCreated());
        long clientId = firstId(mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + j)));

        var guideReq = new CreateGuideRequest(
                campId,
                clientId,
                "000304",
                LocalDate.of(2026, 3, 10),
                null,
                null,
                null,
                List.of(new GuideLineRequest(bookId, 1, new BigDecimal("15.00"))));
        long guideId = objectMapper
                .readTree(mvc.perform(post("/api/v1/guides")
                                .header("Authorization", "Bearer " + j)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(guideReq)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();

        mvc.perform(post("/api/v1/guides/" + guideId + "/installments/plan/custom")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"dueDate\":\"2026-04-10\",\"amount\":15}]}"))
                .andExpect(status().isCreated());

        long instId = objectMapper
                .readTree(mvc.perform(get("/api/v1/guides/" + guideId + "/installments")
                                .header("Authorization", "Bearer " + j))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        mvc.perform(post("/api/v1/guides/" + guideId + "/client-return")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"reason\":\"Devuelto\",\"returnedAt\":null,\"restoreStockToField\":false,\"hideFromReturnList\":false}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/installments/" + instId + "/payments")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5.00,\"paidOn\":\"2026-04-01\",\"note\":null}"))
                .andExpect(status().isConflict());

        mvc.perform(patch("/api/v1/installments/" + instId + "/due-date")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueDate\":\"2026-05-10\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reprogramacionConHistorialYResumenCobranza() throws Exception {
        String j = token("proveedor", "proveedor123");
        long campId = firstId(mvc.perform(get("/api/v1/reference/campaigns").header("Authorization", "Bearer " + j)));
        double campaignPendingBefore = billingSummaryField(j, campId, "totalPending");
        double globalPendingBefore = billingSummaryField(j, null, "totalPending");
        int campaignPendingCountBefore = (int) billingSummaryField(j, campId, "pendingInstallmentCount");
        int campaignPastDueBefore = (int) billingSummaryField(j, campId, "pastDueInstallmentCount");
        long bookId = firstUnitarioBookId(j);
        facturaParaStock(j, bookId, 50, "FAC-BIL-REPRO");

        MvcResult clientRes = mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new biblioteca.gorbits.commercial.dto.ClientRequest("Cobro", "333", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        long clientId =
                objectMapper.readTree(clientRes.getResponse().getContentAsString()).get("id").asLong();

        var guideReq = new CreateGuideRequest(
                campId,
                clientId,
                "000303",
                LocalDate.of(2026, 1, 5),
                null,
                null,
                null,
                List.of(new GuideLineRequest(bookId, 1, new BigDecimal("10.00"))));
        long guideId = objectMapper
                .readTree(mvc.perform(post("/api/v1/guides")
                                .header("Authorization", "Bearer " + j)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(guideReq)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();

        mvc.perform(post("/api/v1/guides/" + guideId + "/installments/plan/custom")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"dueDate\":\"2026-01-01\",\"amount\":10}]}"))
                .andExpect(status().isCreated());

        long instId = objectMapper
                .readTree(mvc.perform(get("/api/v1/guides/" + guideId + "/installments")
                                .header("Authorization", "Bearer " + j))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        mvc.perform(patch("/api/v1/installments/" + instId + "/due-date")
                        .header("Authorization", "Bearer " + j)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueDate\":\"2026-02-15\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/billing/installments/" + instId + "/reschedules")
                        .header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousDueDate").value("2026-01-01"))
                .andExpect(jsonPath("$[0].newDueDate").value("2026-02-15"));

        mvc.perform(get("/api/v1/billing/collections/past-due").header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contractNumber").value("000303"));

        mvc.perform(get("/api/v1/billing/summary")
                        .param("campaignId", String.valueOf(campId))
                        .header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPending").value(campaignPendingBefore + 10.0))
                .andExpect(jsonPath("$.pendingInstallmentCount").value(campaignPendingCountBefore + 1))
                .andExpect(jsonPath("$.pastDueInstallmentCount").value(campaignPastDueBefore + 1));

        mvc.perform(get("/api/v1/billing/summary").header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPending").value(globalPendingBefore + 10.0));

        mvc.perform(get("/api/v1/clients/" + clientId + "/billing-summary")
                        .param("campaignId", String.valueOf(campId))
                        .header("Authorization", "Bearer " + j))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPending").value(10.0))
                .andExpect(jsonPath("$.pendingInstallmentCount").value(1));
    }

    private double billingSummaryField(String jwt, Long campaignId, String field) throws Exception {
        var req = get("/api/v1/billing/summary").header("Authorization", "Bearer " + jwt);
        if (campaignId != null) {
            req = req.param("campaignId", String.valueOf(campaignId));
        }
        JsonNode body = objectMapper.readTree(mvc.perform(req)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return body.get(field).asDouble();
    }

    private long firstUnitarioBookId(String jwt) throws Exception {
        var arr = objectMapper.readTree(mvc.perform(get("/api/v1/catalog/books").header("Authorization", "Bearer " + jwt))
                .andReturn()
                .getResponse()
                .getContentAsString());
        for (var b : arr) {
            if ("UNITARIO".equals(b.get("bookType").asText())) {
                return b.get("id").asLong();
            }
        }
        throw new AssertionError("Sin libro UNITARIO (*) en catálogo");
    }

    private long firstId(org.springframework.test.web.servlet.ResultActions ra) throws Exception {
        return objectMapper
                .readTree(ra.andReturn().getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asLong();
    }

    private void facturaParaStock(String jwt, long bookId, int units, String invoiceNumber) throws Exception {
        String body =
                """
                {"invoiceNumber":"%s","issuedOn":"2026-03-01","note":null,"lines":[{"bookId":%d,"quantity":%d}]}
                """
                        .formatted(invoiceNumber, bookId, units);
        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
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
