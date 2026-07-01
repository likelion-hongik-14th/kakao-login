package mutsa.kakao.auth.config;

import mutsa.kakao.auth.handler.CustomLogoutHandler;
import mutsa.kakao.auth.handler.CustomLogoutSuccessHandler;
import mutsa.kakao.auth.handler.OAuth2AuthenticationFailureHandler;
import mutsa.kakao.auth.handler.OAuth2AuthenticationSuccessHandler;
import mutsa.kakao.auth.security.CustomOAuth2UserService;
import mutsa.kakao.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CustomOAuth2UserService customOAuth2UserService,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			OAuth2AuthenticationSuccessHandler successHandler,
			OAuth2AuthenticationFailureHandler failureHandler,
			CustomLogoutHandler customLogoutHandler,
			CustomLogoutSuccessHandler customLogoutSuccessHandler
	) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/", "/login-success", "/error", "/css/**", "/api/auth/refresh").permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
						.successHandler(successHandler)
						.failureHandler(failureHandler)
				)
				.logout(logout -> logout
						.logoutUrl("/logout")
						.addLogoutHandler(customLogoutHandler)
						.logoutSuccessHandler(customLogoutSuccessHandler)
				)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
