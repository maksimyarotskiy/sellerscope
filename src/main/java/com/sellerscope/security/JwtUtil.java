package com.sellerscope.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

public class JwtUtil {

    // Секретный ключ
    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Срок действия токена: 10 часов
    private final long jwtExpirationMs = 1000 * 60 * 60 * 24 * 14;

    // Генерация токена на основе UserDetails
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    // Извлечение имени пользователя из токена
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Извлечение времени окончания действия токена
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Общий метод извлечения данных из токена
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Проверка: просрочен ли токен
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Проверка, валиден ли токен для пользователя
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Извлечение всех Claims (полей токена)
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
