package mutsa.kakao.auth.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.kakao.auth.domain.KakaoUserProfile;
import mutsa.kakao.auth.util.JwtCookieUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtPrincipalFactory jwtPrincipalFactory;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, JwtPrincipalFactory jwtPrincipalFactory) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtPrincipalFactory = jwtPrincipalFactory;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveAccessToken(request);

		if (token != null
				&& jwtTokenProvider.isValidToken(token)
				&& "access".equals(jwtTokenProvider.getTokenType(token))
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			Long userId = jwtTokenProvider.getUserId(token);
			CustomOAuth2User principal = jwtPrincipalFactory.create(userId);

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}

	private String resolveAccessToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader("Authorization");
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			return authorizationHeader.substring(7);
		}

		Cookie accessCookie = JwtCookieUtils.getCookie(request, JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME);
		return accessCookie == null ? null : accessCookie.getValue();
	}
}
