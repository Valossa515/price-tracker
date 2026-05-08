package io.github.valossa515.pricetracker;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AlertEndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("pricetracker_test")
                    .withUsername("test")
                    .withPassword("test");

    static WireMockServer mlMock = new WireMockServer(0);

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @BeforeAll
    static void startMl() {
        mlMock.start();
    }

    @AfterAll
    static void stopMl() {
        mlMock.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("DB_URL", postgres::getJdbcUrl);
        r.add("DB_USER", postgres::getUsername);
        r.add("DB_PASSWORD", postgres::getPassword);
        r.add("app.marketplace.ml.api-base", () -> mlMock.baseUrl());
        r.add("ML_APP_ID", () -> "test-app");
        r.add("ML_CLIENT_SECRET", () -> "test-secret");
        r.add("COGNITO_ISSUER_URI", () -> "https://test-issuer.invalid/x");
        r.add("MAIL_FROM", () -> "alerts@test.local");
        r.add("MAIL_HOST", () -> "127.0.0.1");
        r.add("MAIL_PORT", () -> ServerSetupTest.SMTP.getPort());
        r.add("MAIL_AUTH", () -> "false");
        r.add("MAIL_TLS", () -> "false");
        r.add("MAIL_USER", () -> "");
        r.add("MAIL_PASSWORD", () -> "");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    AlertRepository repo;

    @BeforeEach
    void reset() throws Exception {
        mlMock.resetAll();
        greenMail.purgeEmailFromAllMailboxes();
        repo.deleteAll();
    }

    @Test
    void shouldCreateAlertTriggerCheckAndDispatchEmail() throws Exception {
        mlMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/oauth/token"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"tok\",\"expires_in\":21600}")));
        mlMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/MLB123"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Test MacBook\"}")));
        mlMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/MLB123/items"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"price\":50.0}]}")));

        RequestPostProcessor authed = jwt().jwt(j ->
                j.subject("user-123").claim("email", "test@example.com"));

        mvc.perform(post("/api/v1/alerts")
                        .with(authed)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"productUrl":"https://www.mercadolivre.com.br/x/p/MLB123",
                                 "productName":"Test MacBook",
                                 "targetPrice":100.00}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/alerts/check-now").with(authed))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.processed").value(1));

        Alert alert = repo.findAll().getFirst();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.TRIGGERED);
        assertThat(alert.getLastObservedPrice()).isEqualByComparingTo("50.00");

        MimeMessage[] msgs = greenMail.getReceivedMessages();
        assertThat(msgs).hasSize(1);
        assertThat(msgs[0].getAllRecipients()[0].toString()).isEqualTo("test@example.com");
        assertThat(msgs[0].getSubject()).contains("Test MacBook");
    }
}
