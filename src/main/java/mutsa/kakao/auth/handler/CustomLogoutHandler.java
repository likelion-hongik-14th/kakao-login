package mutsa.kakao.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.kakao.auth.security.CustomOAuth2User;
import mutsa.kakao.auth.service.AppUserService;
import mutsa.kakao.auth.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutHandler implements LogoutHandler {

	private final AppUserService appUserService;

	@Value("${app.jwt.cookie-secure:false}")
	private boolean cookieSecure;

	public CustomLogoutHandler(AppUserService appUserService) {
		this.appUserService = appUserService;
	}

	@Override
	public void logout(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) {
		if (authentication != null && authentication.getPrincipal() instanceof CustomOAuth2User principal) {
			appUserService.clearRefreshToken(principal.getKakaoUserProfile().id());
		}

		JwtCookieUtils.expireCookie(response, JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME, cookieSecure);
		JwtCookieUtils.expireCookie(response, JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME, cookieSecure);
	}
}
