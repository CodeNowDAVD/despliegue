package biblioteca.gorbits.inventory;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import biblioteca.gorbits.auth.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class LibrarySupplyInvoiceApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminRegistraFacturaEntradaYSubeAlmacen() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = proveedorUserId();

        JsonNode firstBook = firstUnitarioBook(admin);
        long bookId = firstBook.get("id").asLong();
        double expectedAmount = firstBook.get("price").asDouble() * 40;

        int antes = quantityForBook(mvc, admin, bookId);

        String body =
                """
                {"invoiceNumber":"FAC-LIB-001","issuedOn":"2026-03-10","note":"Compra a librería","lines":[{"bookId":%d,"quantity":40}],"ownerUserId":%d}
                """
                        .formatted(bookId, proveedorId);

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("FAC-LIB-001"))
                .andExpect(jsonPath("$.ownerId").value(proveedorId))
                .andExpect(jsonPath("$.totalUnits").value(40))
                .andExpect(jsonPath("$.totalAmount").value(expectedAmount))
                .andExpect(jsonPath("$.lines[0].invoicedQuantity").value(40))
                .andExpect(jsonPath("$.lines[0].netQuantity").value(40));

        int despues = quantityForBook(mvc, admin, bookId);
        assertEquals(antes, despues);
    }

    @Test
    void numeroFacturaDuplicado_devuelve400() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = proveedorUserId();

        long bookId = firstUnitarioBook(admin).get("id").asLong();

        String body =
                """
                {"invoiceNumber":"FAC-DUP-1","issuedOn":"2026-04-01","note":null,"lines":[{"bookId":%d,"quantity":1}],"ownerUserId":%d}
                """
                        .formatted(bookId, proveedorId);

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void facturaPaquete_cantidadesDistintas_rechazada() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = proveedorUserId();

        JsonNode books = objectMapper.readTree(mvc.perform(get("/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long bebidasId = -1;
        long vivirId = -1;
        for (JsonNode b : books) {
            if (b.get("title").asText().contains("Bebidas saludables")) {
                bebidasId = b.get("id").asLong();
            }
            if (b.get("title").asText().contains("Vivir con Esperanza")) {
                vivirId = b.get("id").asLong();
            }
        }
        if (bebidasId < 0 || vivirId < 0) {
            throw new AssertionError("Catálogo de prueba incompleto");
        }

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"invoiceNumber":"FAC-PAQ-BAD","issuedOn":"2026-05-01","note":null,"lines":[{"bookId":%d,"quantity":5},{"bookId":%d,"quantity":6}],"ownerUserId":%d}
                                """
                                        .formatted(bebidasId, vivirId, proveedorId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListaFacturasConYsinFiltroProveedor() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = proveedorUserId();
        long bookId = firstUnitarioBook(admin).get("id").asLong();

        String body =
                """
                {"invoiceNumber":"FAC-LIST-01","issuedOn":"2026-03-15","note":null,"lines":[{"bookId":%d,"quantity":2}],"ownerUserId":%d}
                """
                        .formatted(bookId, proveedorId);
        JsonNode created = objectMapper.readTree(mvc.perform(post("/api/v1/inventory/library-invoices")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long invoiceId = created.get("id").asLong();

        mvc.perform(get("/api/v1/inventory/library-invoices").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.invoiceNumber=='FAC-LIST-01')]").exists());

        mvc.perform(get("/api/v1/inventory/library-invoices")
                        .param("providerId", String.valueOf(proveedorId))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + invoiceId + ")]").exists());

        mvc.perform(get("/api/v1/inventory/library-invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("FAC-LIST-01"));
    }

    @Test
    void proveedorListaYConsultaSusFacturas() throws Exception {
        String prov = token("proveedor", "proveedor123");
        long bookId = firstUnitarioBook(prov).get("id").asLong();

        JsonNode created = objectMapper.readTree(mvc.perform(post("/api/v1/inventory/library-invoices")
                                .header("Authorization", "Bearer " + prov)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"invoiceNumber":"FAC-PROV-LIST","issuedOn":"2026-03-20","note":null,"lines":[{"bookId":%d,"quantity":3}]}
                                        """
                                                .formatted(bookId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long invoiceId = created.get("id").asLong();

        mvc.perform(get("/api/v1/inventory/library-invoices").header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.invoiceNumber=='FAC-PROV-LIST')]").exists());

        mvc.perform(get("/api/v1/inventory/library-invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + prov))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("FAC-PROV-LIST"));
    }

    @Test
    void facturaReplicaRemision_conTituloDobleAsterisco_dosLineasExplicitas() throws Exception {
        String admin = token("admin", "admin123");
        long proveedorId = proveedorUserId();

        JsonNode books = objectMapper.readTree(mvc.perform(get("/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long bebidasId = -1;
        long vivirId = -1;
        for (JsonNode b : books) {
            if (b.get("title").asText().contains("Bebidas saludables")) {
                bebidasId = b.get("id").asLong();
            }
            if (b.get("title").asText().contains("Vivir con Esperanza")) {
                vivirId = b.get("id").asLong();
            }
        }
        if (bebidasId < 0 || vivirId < 0) {
            throw new AssertionError("Catálogo de prueba sin Bebidas saludables / Vivir con Esperanza");
        }

        mvc.perform(post("/api/v1/inventory/library-invoices")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"invoiceNumber":"FAC-PAQ-01","issuedOn":"2026-05-01","note":null,"lines":[{"bookId":%d,"quantity":10},{"bookId":%d,"quantity":10}],"ownerUserId":%d}
                                """
                                        .formatted(bebidasId, vivirId, proveedorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(265.0))
                .andExpect(jsonPath("$.totalUnits").value(20))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    private JsonNode firstUnitarioBook(String adminJwt) throws Exception {
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + adminJwt))
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

    private int quantityForBook(MockMvc mvc, String adminJwt, long bookId) throws Exception {
        JsonNode arr = objectMapper.readTree(mvc.perform(get("/api/v1/inventory/warehouse")
                                .header("Authorization", "Bearer " + adminJwt))
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

    private long proveedorUserId() throws Exception {
        String prov = token("proveedor", "proveedor123");
        return objectMapper.readTree(mvc.perform(get("/api/v1/me")
                                .header("Authorization", "Bearer " + prov))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();
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
