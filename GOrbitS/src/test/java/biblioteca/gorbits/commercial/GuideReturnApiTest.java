package biblioteca.gorbits.commercial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class GuideReturnApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void devolucionRestauraStockYApareceEnLista() throws Exception {
        String prov = token("proveedor", "proveedor123");

        long bookId = firstUnitarioBookId(prov);

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"invoiceNumber\":\"FAC-RET-01\",\"issuedOn\":\"2026-03-01\",\"note\":null,\"lines\":[{\"bookId\":"
                                        + bookId + ",\"quantity\":10}]}"))
                .andExpect(status().isCreated());

        long campId =
                objectMapper.readTree(mvc.perform(get("/api/v1/reference/campaigns").header("Authorization", "Bearer " + prov))
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                        .get(0)
                        .get("id")
                        .asLong();

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClientRequest("Devolución", "555", null, null))))
                .andExpect(status().isCreated());
        long clientId = objectMapper.readTree(mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + prov))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        var guideReq =
                new CreateGuideRequest(
                        campId,
                        clientId,
                        "000401",
                        LocalDate.of(2026, 4, 1),
                        GuideStatus.ACTIVA,
                        null,
                        null,
                        List.of(new GuideLineRequest(bookId, 2, new BigDecimal("11.00"))));

        MvcResult gr = mvc.perform(post("/api/v1/guides")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guideReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long guideId = objectMapper.readTree(gr.getResponse().getContentAsString()).get("id").asLong();

        int disponibleAntes = disponible(prov, bookId);
        assertEquals(8, disponibleAntes);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("reason", "Cliente devolvió material");
        payload.putNull("returnedAt");
        payload.put("restoreStockToField", true);
        payload.put("hideFromReturnList", false);

        mvc.perform(post("/api/v1/guides/" + guideId + "/client-return")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEVUELTA"))
                .andExpect(jsonPath("$.clientReturn.reason").value("Cliente devolvió material"));

        assertEquals(10, disponible(prov, bookId));

        mvc.perform(get("/api/v1/guides/returns").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(post("/api/v1/guides/" + guideId + "/client-return")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isConflict());

        mvc.perform(patch("/api/v1/guides/" + guideId + "/status")
                        .header("Authorization", "Bearer " + prov)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVA\"}"))
                .andExpect(status().isConflict());
    }

    private int disponible(String jwt, long bookId) throws Exception {
        String json = mvc.perform(get("/api/v1/inventory/my-stock").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var arr = objectMapper.readTree(json);
        for (var n : arr) {
            if (n.get("bookId").asLong() == bookId) {
                return n.get("available").asInt();
            }
        }
        return 0;
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

    private String token(String user, String pass) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user, pass))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
