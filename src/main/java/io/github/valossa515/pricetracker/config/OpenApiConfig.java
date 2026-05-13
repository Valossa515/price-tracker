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
                                
                                ## Authentication
                                
                                All endpoints require a Bearer JWT issued by AWS Cognito.
                                The public API additionally requires the OAuth2 scope
                                `pricetracker/external-api`.
                                
                                ### How to request credentials
                                
                                Credentials (`client_id` + `client_secret`) are issued manually.
                                Email **fe.mmo515@gmail.com** with:
                                
                                - Company / project name
                                - Intended use case
                                - Expected request volume
                                - Source IP range (optional, for allowlisting)
                                
                                You will receive the credentials via a secure share link.
                                
                                ### How to get a token (client_credentials)
                                
                                Request a token from your Cognito user pool using the
                                `client_id` and `client_secret` provided to your integration:
                                
                                ```
                                curl -u <CLIENT_ID>:<CLIENT_SECRET> \\
                                  -d "grant_type=client_credentials&scope=pricetracker/external-api" \\
                                  https://price-tracker-dev-3fde8e2b.auth.us-east-1.amazoncognito.com/oauth2/token
                                ```
                                
                                The response contains `access_token`. Click **Authorize**
                                above and paste the value (without the `Bearer ` prefix).
                                Tokens are valid for 1 hour by default.
                                
                                ## Rate limit
                                
                                Public endpoints are rate-limited per JWT subject
                                (default 60 req/min, burst 10). Exceeded requests return
                                HTTP 429 with `Retry-After: 60`.
                                
                                ## Webhooks
                                
                                When you create an alert with `webhookUrl` and `webhookSecret`,
                                triggered events are POSTed as JSON with the headers:
                                
                                - `X-PriceTracker-Event`
                                - `X-PriceTracker-Timestamp`
                                - `X-PriceTracker-Signature: sha256=<hex HMAC-SHA256 of body using your secret>`
                                
                                Verify the signature on your side before trusting the payload.
                                """)
                        .contact(new Contact().name("price-tracker").email("fe.mmo515@gmail.com"))
                        .license(new License().name("Proprietary")))
                .components(new Components())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
