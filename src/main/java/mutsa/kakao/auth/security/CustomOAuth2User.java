package mutsa.kakao.auth.security;

import java.util.Collection;
import java.util.Map;

import mutsa.kakao.auth.domain.KakaoUserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

	private final Collection<? extends GrantedAuthority> authorities;
	private final Map<String, Object> attributes;
	private final String nameAttributeKey;
	private final KakaoUserProfile kakaoUserProfile;

	public CustomOAuth2User(
			Collection<? extends GrantedAuthority> authorities,
			Map<String, Object> attributes,
			String nameAttributeKey,
			KakaoUserProfile kakaoUserProfile
	) {
		this.authorities = authorities;
		this.attributes = attributes;
		this.nameAttributeKey = nameAttributeKey;
		this.kakaoUserProfile = kakaoUserProfile;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getName() {
		Object name = attributes.get(nameAttributeKey);
		return name == null ? kakaoUserProfile.providerId() : String.valueOf(name);
	}

	public KakaoUserProfile getKakaoUserProfile() {
		return kakaoUserProfile;
	}
}
