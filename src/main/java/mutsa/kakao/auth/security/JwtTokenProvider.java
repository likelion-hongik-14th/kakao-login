package mutsa.kakao.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mutsa.kakao.auth.domain.KakaoUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long accessTokenExpiration;
	private final long refreshTokenExpiration;

	public JwtTokenProvider(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
			@Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration
	) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpiration = accessTokenExpiration;
		this.refreshTokenExpiration = refreshTokenExpiration;
	}

	public String createAccessToken(KakaoUserProfile userProfile) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(accessTokenExpiration);

		return Jwts.builder()
				.subject(String.valueOf(userProfile.id()))
				.claim("provider", userProfile.provider())
				.claim("nickname", userProfile.nickname())
				.claim("type", "access")
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(secretKey)
				.compact();
	}

	public String createRefreshToken(KakaoUserProfile userProfile) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(refreshTokenExpiration);

		return Jwts.builder()
				.subject(String.valueOf(userProfile.id()))
				.claim("type", "refresh")
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(secretKey)
				.compact();
	}

	public boolean isValidToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	public Long getUserId(String token) {
		return Long.valueOf(parseClaims(token).getSubject());
	}

	public String getTokenType(String token) {
		return parseClaims(token).get("type", String.class);
	}

	public LocalDateTime getRefreshTokenExpiry(String token) {
		Date expiration = parseClaims(token).getExpiration();
		return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
	}

	public long getAccessTokenExpiration() {
		return accessTokenExpiration;
	}

	public long getRefreshTokenExpiration() {
		return refreshTokenExpiration;
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
