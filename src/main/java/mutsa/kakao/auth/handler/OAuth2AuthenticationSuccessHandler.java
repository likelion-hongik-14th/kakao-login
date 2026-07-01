package mutsa.kakao.auth.handler;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.kakao.auth.dto.AuthTokenResponse;
import mutsa.kakao.auth.security.CustomOAuth2User;
import mutsa.kakao.auth.service.JwtAuthService;
import mutsa.kakao.auth.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtAuthService jwtAuthService;

	@Value("${app.jwt.cookie-secure:false}")
	private boolean cookieSecure;

	public OAuth2AuthenticationSuccessHandler(JwtAuthService jwtAuthService) {
		this.jwtAuthService = jwtAuthService;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
		AuthTokenResponse tokenResponse = jwtAuthService.issueTokens(principal.getKakaoUserProfile());

		JwtCookieUtils.addTokenCookie(
				response,
				JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME,
				tokenResponse.accessToken(),
				tokenResponse.accessTokenExpiresIn() / 1000,
				cookieSecure
		);
		JwtCookieUtils.addTokenCookie(
				response,
				JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME,
				tokenResponse.refreshToken(),
				tokenResponse.refreshTokenExpiresIn() / 1000,
				cookieSecure
		);

		getRedirectStrategy().sendRedirect(request, response, "/login-success");
	}
}
