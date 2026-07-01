package mutsa.kakao.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public final class JwtCookieUtils {

	public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
	public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

	private JwtCookieUtils() {
	}

	public static void addTokenCookie(
			HttpServletResponse response,
			String name,
			String value,
			long maxAgeSeconds,
			boolean secure
	) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(secure)
				.sameSite("Lax")
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public static void expireCookie(HttpServletResponse response, String name, boolean secure) {
		ResponseCookie cookie = ResponseCookie.from(name, "")
				.httpOnly(true)
				.secure(secure)
				.sameSite("Lax")
				.path("/")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public static Cookie getCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}

		return null;
	}
}
