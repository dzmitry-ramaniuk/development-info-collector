package example.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import({ApiSecurityConfig.class, OrderSecurityTest.TestJwtConfig.class})
class OrderSecurityTest {
    static final String ISSUER = "https://issuer.example.test";
    static final String AUDIENCE = "orders-api";

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired JwtAuthenticationConverter converter;

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/api/orders/42")).andExpect(status().isUnauthorized());
    }

    @Test
    void roleWithoutScopeIsForbidden() throws Exception {
        String jwt = token(encoder, ISSUER, AUDIENCE, "", List.of("USER"), 300);
        mvc.perform(get("/api/orders/42").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isForbidden());
    }

    @Test
    void signedTokenWithScopeCanReadOrder() throws Exception {
        String jwt = token(encoder, ISSUER, AUDIENCE, "orders.read", List.of("USER"), 300);
        mvc.perform(get("/api/orders/42").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());
    }

    @Test
    void rolesAndScopesArePreservedTogether() {
        Jwt jwt = decoder.decode(token(encoder, ISSUER, AUDIENCE,
            "orders.read orders.cancel", List.of("ADMIN"), 300));
        assertThat(converter.convert(jwt).getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactlyInAnyOrder("SCOPE_orders.read", "SCOPE_orders.cancel", "ROLE_ADMIN");
    }

    @Test
    void adminRoleOpensAdminEndpoint() throws Exception {
        String jwt = token(encoder, ISSUER, AUDIENCE, "", List.of("ADMIN"), 300);
        mvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());
    }

    @Test
    void scopeDoesNotGrantAdminRole() throws Exception {
        String jwt = token(encoder, ISSUER, AUDIENCE, "orders.read", List.of("USER"), 300);
        mvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isForbidden());
    }

    @Test
    void incorrectIssuerIsUnauthorized() throws Exception {
        assertUnauthorized(token(encoder, "https://other.example.test", AUDIENCE,
            "orders.read", List.of(), 300));
    }

    @Test
    void incorrectAudienceIsUnauthorized() throws Exception {
        assertUnauthorized(token(encoder, ISSUER, "another-api", "orders.read", List.of(), 300));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        assertUnauthorized(token(encoder, ISSUER, AUDIENCE, "orders.read", List.of(), -120));
    }

    @Test
    void tokenSignedByAnotherKeyIsUnauthorized() throws Exception {
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("other-key").generate();
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(otherKey)));
        assertUnauthorized(token(otherEncoder, ISSUER, AUDIENCE, "orders.read", List.of(), 300));
    }

    private void assertUnauthorized(String jwt) throws Exception {
        mvc.perform(get("/api/orders/42").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isUnauthorized());
    }

    private String token(JwtEncoder signer, String issuer, String audience,
            String scope, List<String> roles, long expirationSeconds) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject("alice")
            .audience(List.of(audience)).issuedAt(now.minusSeconds(600))
            .expiresAt(now.plusSeconds(expirationSeconds))
            .claim("scope", scope).claim("roles", roles).build();
        return signer.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    @TestConfiguration
    static class TestJwtConfig {
        @Bean
        RSAKey testKey() throws JOSEException {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        }

        @Bean
        JwtEncoder testEncoder(RSAKey testKey) {
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(testKey)));
        }

        @Bean
        JwtDecoder testDecoder(RSAKey testKey) throws JOSEException {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(testKey.toRSAPublicKey()).build();
            OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(AUDIENCE)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER), audience));
            return decoder;
        }
    }
}
