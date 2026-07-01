package mutsa.kakao.auth.domain;

import java.time.LocalDateTime;

public record KakaoUserProfile(
		Long id,
		String provider,
		String providerId,
		String nickname,
		String email,
		String profileImage,
		LocalDateTime lastLoginAt,
		boolean newlyRegistered
) {

	public static KakaoUserProfile from(AppUser user, boolean newlyRegistered) {
		return new KakaoUserProfile(
				user.getId(),
				user.getProvider(),
				user.getProviderId(),
				user.getNickname(),
				user.getEmail(),
				user.getProfileImage(),
				user.getLastLoginAt(),
				newlyRegistered
		);
	}
}
