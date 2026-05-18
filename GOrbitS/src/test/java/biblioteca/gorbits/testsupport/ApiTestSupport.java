package biblioteca.gorbits.testsupport;

import biblioteca.gorbits.auth.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Utilidades compartidas para tests de integración API. */
public final class ApiTestSupport {

    public static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private ApiTestSupport() {}

    public static String login(MockMvc mvc, String user, String pass) throws Exception {
        MvcResult r = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON.writeValueAsString(new LoginRequest(user, pass))))
                .andReturn();
        return JSON.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    public static long firstId(JsonNode array) {
        return array.get(0).get("id").asLong();
    }

    public static long firstUnitarioBookId(MockMvc mvc, String jwt) throws Exception {
        JsonNode arr = JSON.readTree(mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/catalog/books")
                                .header("Authorization", "Bearer " + jwt))
                .andReturn()
                .getResponse()
                .getContentAsString());
        for (JsonNode b : arr) {
            if ("UNITARIO".equals(b.get("bookType").asText())) {
                return b.get("id").asLong();
            }
        }
        throw new AssertionError("Sin libro UNITARIO en catálogo");
    }

    public static void facturaLibreria(MockMvc mvc, String jwt, long bookId, int qty, String invoiceNo)
            throws Exception {
        String body =
                """
                {"invoiceNumber":"%s","issuedOn":"2026-03-01","note":null,"lines":[{"bookId":%d,"quantity":%d}]}
                """
                        .formatted(invoiceNo, bookId, qty);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/inventory/library-invoices")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isCreated());
    }
}
