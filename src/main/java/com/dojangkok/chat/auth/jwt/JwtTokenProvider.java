package com.dojangkok.chat.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYP = "typ";
    private static final String TYP_ACCESS = "access";

    private final SecretKey key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretBase64) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public Optional<String> tryExtractUserIdFromAccessToken(String token) {
        try {
            Claims claims = parseAndValidateType(token);
            return Optional.of(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.debug("만료된 access 토큰: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 access 토큰: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Claims parseAndValidateType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get(CLAIM_TYP, String.class);
        if (!TYP_ACCESS.equals(type)) {
            throw new JwtException("Invalid token type: expected=access, actual=" + type);
        }

        return claims;
    }
}
