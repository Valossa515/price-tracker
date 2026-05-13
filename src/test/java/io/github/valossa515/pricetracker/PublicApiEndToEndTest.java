package io.github.valossa515.pricetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PublicApiEndToEndTest {

    static final String SCOPE = "external-api";
    static final String WEBHOOK_SECRET = "supersecret-supersecret";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("pricetracker_test")
                    .withUsername("test")
                    .withPassword("test");

    static WireMockServer mlMock = new WireMockServer(0);
    static WireMockServer webhookMock = new WireMockServer(0);

    @BeforeAll
    static void startMocks() {
        mlMock.start();
        webhookMock.start();
    }

    @AfterAll
    static void stopMocks() {
        mlMock.stop();
        webhookMock.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("DB_URL", postgres::getJdbcUrl);
        r.add("DB_USER", postgres::getUsername);
        r.add("DB_PASSWORD", postgres::getPassword);
        r.add("app.marketplace.ml.api-base", mlMock::baseUrl);
        r.add("ML_APP_ID", () -> "test-app");
        r.add("ML_CLIENT_SECRET", () -> "test-secret");
        r.add("COGNITO_ISSUER_URI", () -> "https://test-issuer.invalid/x");
        r.add("MAIL_FROM", () -> "alerts@test.local");
        r.add("MAIL_HOST", () -> "127.0.0.1");
        r.add("MAIL_PORT", () -> "3025");
        r.add("MAIL_AUTH", () -> "false");
        r.add("MAIL_TLS", () -> "false");
        r.add("MAIL_USER", () -> "");
        r.add("MAIL_PASSWORD", () -> "");
        // Tight rate limit so the rate-limit test stays fast/deterministic.
        r.add("PUBLIC_API_RPM", () -> "3");
        r.add("PUBLIC_API_BURST", () -> "3");
        r.add("PUBLIC_API_WEBHOOK_MAX_ATTEMPTS", () -> "1");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    AlertRepository repo;

    final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void reset() {
        mlMock.resetAll();
        webhookMock.resetAll();
        repo.deleteAll();
    }

    private RequestPostProcessor jwtWith(String sub, String scope) {
        var pp = jwt().jwt(j -> j.subject(sub));
        if (scope != null) {
            pp = jwt().jwt(j -> j.subject(sub).claim("scope", scope));
        }
        return pp;
    }

    @Test
    void rejectsRequestWithoutScope() throws Exception {
        mvc.perform(get("/api/v1/public/alerts").with(jwtWith("svc-no-scope", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsRequestWithCorrectScope() throws Exception {
        mvc.perform(get("/api/v1/public/alerts").with(jwtWith("svc-ok", SCOPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void rateLimitsAfterBurst() throws Exception {
        var auth = jwtWith("svc-burst", SCOPE);
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/v1/public/alerts").with(auth))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/public/alerts").with(auth))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void createAlertAndDeliversSignedWebhookOnTrigger() throws Exception {
        stubMlPrice("MLB999", 50.0);
        webhookMock.stubFor(WireMock.post("/hook").willReturn(WireMock.aResponse().withStatus(200)));

        String webhookUrl = webhookMock.baseUrl() + "/hook";
        String body = """
                {"productUrl":"https://www.mercadolivre.com.br/x/p/MLB999",
                 "productName":"Hook Product",
                 "targetPrice":100.00,
                 "ownerEmail":"partner@example.com",
                 "webhookUrl":"%s",
                 "webhookSecret":"%s"}
                """.formatted(webhookUrl, WEBHOOK_SECRET);

        var auth = jwtWith("svc-webhook", SCOPE);
        MvcResult created = mvc.perform(post("/api/v1/public/alerts")
                        .with(auth).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.webhookConfigured").value(true))
                .andReturn();

        UUID alertId = UUID.fromString(
                json.readTree(created.getResponse().getContentAsString()).get("id").asText());
        Alert stored = repo.findById(alertId).orElseThrow();
        assertThat(stored.getWebhookUrl()).isEqualTo(webhookUrl);
        assertThat(stored.getUserId()).isEqualTo("svc-webhook");

        // Trigger price check (uses internal endpoint with a user-style JWT — no scope needed).
        mvc.perform(post("/api/v1/alerts/check-now")
                        .with(jwt().jwt(j -> j.subject("svc-webhook").claim("email", "ignored@x"))))
                .andExpect(status().isAccepted());

        assertThat(repo.findById(alertId).orElseThrow().getStatus()).isEqualTo(AlertStatus.TRIGGERED);

        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            List<LoggedRequest> hits = webhookMock.findAll(
                    RequestPatternBuilder.newRequestPattern(
                            com.github.tomakehurst.wiremock.http.RequestMethod.POST,
                            WireMock.urlEqualTo("/hook")));
            assertThat(hits).hasSize(1);
            LoggedRequest req = hits.getFirst();

            String sig = req.getHeader("X-PriceTracker-Signature");
            String event = req.getHeader("X-PriceTracker-Event");
            String payload = req.getBodyAsString();

            assertThat(event).isEqualTo("price.target_reached");
            assertThat(sig).isEqualTo(hmac(WEBHOOK_SECRET, payload));

            JsonNode node = json.readTree(payload);
            assertThat(node.get("alertId").asText()).isEqualTo(alertId.toString());
            assertThat(node.get("event").asText()).isEqualTo("price.target_reached");
            assertThat(node.get("observedPrice").decimalValue()).isEqualByComparingTo("50.00");
        });
    }

    @Test
    void deleteAlertViaPublicApi() throws Exception {
        Alert seeded = repo.save(Alert.builder()
                .userId("svc-del").userEmail("x@y.z")
                .productUrl("https://www.mercadolivre.com.br/x/p/MLBDEL")
                .alertType(io.github.valossa515.pricetracker.alert.AlertType.PRICE_BELOW_TARGET)
                .targetPrice(new java.math.BigDecimal("10.00"))
                .build());
        var auth = jwtWith("svc-del", SCOPE);
        mvc.perform(delete("/api/v1/public/alerts/" + seeded.getId()).with(auth))
                .andExpect(status().isNoContent());
        assertThat(repo.findById(seeded.getId())).isEmpty();
    }

    private void stubMlPrice(String mlbId, double price) {
        mlMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/oauth/token"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"tok\",\"expires_in\":21600}")));
        mlMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/" + mlbId))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Hook Product\"}")));
        mlMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/" + mlbId + "/items"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"price\":" + price + "}]}")));
    }

    private static String hmac(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(digest);
    }
}
