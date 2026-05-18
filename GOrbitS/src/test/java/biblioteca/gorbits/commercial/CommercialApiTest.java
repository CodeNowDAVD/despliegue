package biblioteca.gorbits.commercial;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import biblioteca.gorbits.commercial.dto.PatchGuideStatusRequest;
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
class CommercialApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    WebApplicationContext context;

    @Autowired
    BookRepository books;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void proveedorFlujoClienteYGuia() throws Exception {
        String jwt = token("proveedor", "proveedor123");

        long bookId = books.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        long campaignId =
                objectMapper.readTree(mvc.perform(get("/api/v1/reference/campaigns").header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                        .get(0)
                        .get("id")
                        .asLong();

        facturaParaStock(jwt, bookId, 50, "FAC-COM-01");

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClientRequest("María Pérez", "999888777", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        MvcResult clientRes = mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        long clientId = objectMapper.readTree(clientRes.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        var guideReq = new CreateGuideRequest(
                campaignId,
                clientId,
                "000201",
                LocalDate.of(2026, 3, 10),
                GuideStatus.ACTIVA,
                null,
                null,
                List.of(new GuideLineRequest(bookId, 2, new BigDecimal("15.00"))));

        mvc.perform(post("/api/v1/guides")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guideReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").exists());

        long guideId = objectMapper.readTree(
                        mvc.perform(get("/api/v1/guides").header("Authorization", "Bearer " + jwt))
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        mvc.perform(get("/api/v1/guides/" + guideId).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guideId))
                .andExpect(jsonPath("$.contractNumber").value("000201"));

        mvc.perform(patch("/api/v1/guides/" + guideId + "/status")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatchGuideStatusRequest(GuideStatus.CERRADA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CERRADA"));
    }

    @Test
    void crearContratoSinStock_devuelve400() throws Exception {
        String jwt = token("proveedor", "proveedor123");

        long bookId = books.findAll().stream().findFirst().orElseThrow().getId();
        long campaignId = objectMapper.readTree(mvc.perform(get("/api/v1/reference/campaigns")
                                .header("Authorization", "Bearer " + jwt))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        mvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClientRequest("Sin stock", "600", null, null))))
                .andExpect(status().isCreated());
        long clientId = objectMapper.readTree(mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + jwt))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();

        var guideReq = new CreateGuideRequest(
                campaignId,
                clientId,
                "000202",
                LocalDate.of(2026, 3, 11),
                GuideStatus.ACTIVA,
                null,
                null,
                List.of(new GuideLineRequest(bookId, 1, new BigDecimal("10.00"))));

        mvc.perform(post("/api/v1/guides")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guideReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void adminNoAccedeAClientes() throws Exception {
        String jwt = token("admin", "admin123");
        mvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    private String token(String user, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
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
}
