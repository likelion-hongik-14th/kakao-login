package mutsa.kakao.auth.dto;

public record AuthTokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
