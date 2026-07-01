package mutsa.kakao.auth.service;

import java.time.LocalDateTime;

import mutsa.kakao.auth.domain.AppUser;
import mutsa.kakao.auth.domain.KakaoUserProfile;
import mutsa.kakao.auth.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

	private final AppUserRepository appUserRepository;

	public AppUserService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Transactional
	public KakaoUserProfile saveOrUpdate(
			String provider,
			String providerId,
			String nickname,
			String email,
			String profileImage
	) {
		return appUserRepository.findByProviderAndProviderId(provider, providerId)
				.map(existingUser -> {
					existingUser.updateProfile(nickname, email, profileImage);
					return KakaoUserProfile.from(existingUser, false);
				})
				.orElseGet(() -> {
					AppUser newUser = new AppUser(provider, providerId, nickname, email, profileImage);
					AppUser savedUser = appUserRepository.save(newUser);
					return KakaoUserProfile.from(savedUser, true);
				});
	}

	@Transactional(readOnly = true)
	public KakaoUserProfile findProfileById(Long userId) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		return KakaoUserProfile.from(user, false);
	}

	@Transactional(readOnly = true)
	public AppUser findByRefreshToken(String refreshToken) {
		return appUserRepository.findByRefreshToken(refreshToken)
				.orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
	}

	@Transactional
	public void updateRefreshToken(Long userId, String refreshToken, LocalDateTime expiresAt) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		user.updateRefreshToken(refreshToken, expiresAt);
	}

	@Transactional
	public void clearRefreshToken(Long userId) {
		appUserRepository.findById(userId)
				.ifPresent(AppUser::clearRefreshToken);
	}
}
