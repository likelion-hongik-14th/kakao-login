package mutsa.kakao.auth.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.kakao.auth.domain.KakaoUserProfile;
import mutsa.kakao.auth.dto.AuthTokenResponse;
import mutsa.kakao.auth.security.CustomOAuth2User;
import mutsa.kakao.auth.service.JwtAuthService;
import mutsa.kakao.auth.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {

	private final JwtAuthService jwtAuthService;
	private final boolean cookieSecure;

	public AuthController(
			JwtAuthService jwtAuthService,
			@Value("${app.jwt.cookie-secure:false}") boolean cookieSecure
	) {
		this.jwtAuthService = jwtAuthService;
		this.cookieSecure = cookieSecure;
	}

	@GetMapping("/")
	public String home(HttpServletRequest request, Model model) {
		model.addAttribute("error", request.getParameter("error"));
		model.addAttribute("message", request.getParameter("message"));
		model.addAttribute("logout", request.getParameter("logout"));
		return "index";
	}

	@GetMapping("/login-success")
	public String loginSuccess(
			@AuthenticationPrincipal CustomOAuth2User user,
			Model model
	) {
		KakaoUserProfile profile = user.getKakaoUserProfile();
		model.addAttribute("user", profile);
		model.addAttribute("pageTitle", "멋사 방학세션 #3 - 카카오로그인 테스트");
		model.addAttribute("welcomeMessage", profile.newlyRegistered()
				? "카카오 계정으로 회원가입이 완료되었습니다."
				: "카카오 계정으로 로그인되었습니다.");
		model.addAttribute("tokenModeMessage", "JWT Access Token과 Refresh Token이 HttpOnly 쿠키로 발급되었습니다.");
		return "login-success";
	}

	@GetMapping("/api/me")
	@ResponseBody
	public KakaoUserProfile me(@AuthenticationPrincipal CustomOAuth2User user) {
		return user.getKakaoUserProfile();
	}

	@PostMapping("/api/auth/refresh")
	@ResponseBody
	public AuthTokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		Cookie refreshTokenCookie = JwtCookieUtils.getCookie(request, JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME);
		if (refreshTokenCookie == null) {
			throw new IllegalArgumentException("Refresh token cookie is missing");
		}

		AuthTokenResponse tokenResponse = jwtAuthService.refresh(refreshTokenCookie.getValue());
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
		return tokenResponse;
	}
}
