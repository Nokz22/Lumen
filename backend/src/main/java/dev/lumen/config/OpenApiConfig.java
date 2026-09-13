package dev.lumen.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The published contract doubles as the ethical boundary's most public statement: anyone
 * reading the API — not just the UI — has to see that this is a wellbeing tool and not a
 * diagnostic one, so ADR-0001 is restated in the description rather than only in the docs.
 *
 * <p>Authentication is described as an httpOnly cookie (ADR-0004) rather than a bearer
 * token, which also means "Try it out" only works from a browser session that has already
 * logged in — the token is deliberately not reachable from JavaScript.
 */
@Configuration
public class OpenApiConfig {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    @Bean
    public OpenAPI lumenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lumen API")
                        .version("v1")
                        .description(
                                """
                                Wellbeing and self-care platform.

                                **Lumen is not a medical device.** It does not diagnose, does not treat and does \
                                not replace professional care, and it is intended for adults (18+) only. Scores are \
                                expressed in wellbeing language throughout — never as diagnostic labels (ADR-0001).

                                Any positive answer to PHQ-9 item 9 halts scoring and triggers the crisis flow \
                                before a score is ever returned (ADR-0006).""")
                        .contact(new Contact().name("Nuno Ferreira").url("https://github.com/Nokz22/Lumen"))
                        .license(new License().name("MIT").url("https://github.com/Nokz22/Lumen/blob/main/LICENSE")))
                .components(new Components()
                        .addSecuritySchemes(
                                ACCESS_TOKEN_COOKIE,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name(ACCESS_TOKEN_COOKIE)
                                        .description("httpOnly JWT access token, issued by POST /api/v1/auth/login"
                                                + " and rotated by POST /api/v1/auth/refresh (ADR-0004).")));
    }
}
