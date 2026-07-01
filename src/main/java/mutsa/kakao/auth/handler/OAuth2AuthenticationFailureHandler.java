package mutsa.kakao.auth.handler;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {
		String redirectUrl = UriComponentsBuilder.fromPath("/")
				.queryParam("error", "oauth2_login_failed")
				.queryParam("message", exception.getMessage())
				.build()
				.toUriString();

		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}
}
