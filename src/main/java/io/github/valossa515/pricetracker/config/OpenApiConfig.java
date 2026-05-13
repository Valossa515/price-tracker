package io.github.valossa515.pricetracker.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    OpenAPI priceTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("price-tracker API")
                        .version("v1")
                        .description("""
                                Internal endpoints under `/api/v1/*` and the external
                                consumer-facing API under `/api/v1/public/*`.
                                
                                **Authentication.** All endpoints require a Bearer JWT
                                issued by AWS Cognito. The public API additionally requires
                                the OAuth2 scope configured by `app.publicapi.required-scope`
                                (default `external-api`).
                                
                                **Rate limit.** Public endpoints are rate-limited per JWT
                                subject (default 60 req/min, burst 10).
                                
                                **Webhooks.** When you create an alert with `webhookUrl` and
                                `webhookSecret`, triggered events are POSTed as JSON with the
                                headers `X-PriceTracker-Event`, `X-PriceTracker-Timestamp`,
                                and `X-PriceTracker-Signature: sha256=<hex HMAC of body>`.
                                """)
                        .contact(new Contact().name("price-tracker"))
                        .license(new License().name("Proprietary")))
                .components(new Components())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
