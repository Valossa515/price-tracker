package io.github.valossa515.pricetracker.config;

import io.github.valossa515.pricetracker.publicapi.PublicApiProperties;
import io.github.valossa515.pricetracker.publicapi.PublicApiRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    private final CorsProperties corsProperties;
    private final PublicApiProperties publicApiProperties;
    private final PublicApiRateLimitFilter publicApiRateLimitFilter;

    public SecurityConfig(CorsProperties corsProperties,
                          PublicApiProperties publicApiProperties,
                          PublicApiRateLimitFilter publicApiRateLimitFilter) {
        this.corsProperties = corsProperties;
        this.publicApiProperties = publicApiProperties;
        this.publicApiRateLimitFilter = publicApiRateLimitFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String publicScope = "SCOPE_" + publicApiProperties.requiredScope();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/public/**").hasAuthority(publicScope)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                // Run rate limit AFTER authorization so we can key on the JWT subject
                // and only count requests that already passed scope checks.
                .addFilterAfter(publicApiRateLimitFilter, AuthorizationFilter.class)
                .headers(h -> h
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .referrerPolicy(rp -> rp
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                );
        return http.build();
    }

    /**
     * Prevents Spring Boot from auto-registering {@link PublicApiRateLimitFilter}
     * on the servlet container's main filter chain. The filter is added explicitly
     * inside the Spring Security chain (after authorization) so it can read the
     * authenticated JWT subject.
     */
    @Bean
    FilterRegistrationBean<PublicApiRateLimitFilter> disablePublicApiRateLimitAutoRegistration(
            PublicApiRateLimitFilter filter) {
        FilterRegistrationBean<PublicApiRateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        List<String> patterns = Arrays.stream((corsProperties.allowedOriginPatterns() == null ? "" : corsProperties.allowedOriginPatterns()).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        log.info("CORS allowed origin patterns: {}", patterns);
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
