package mutsa.kakao.auth.repository;

import java.util.Optional;

import mutsa.kakao.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByProviderAndProviderId(String provider, String providerId);

	Optional<AppUser> findByRefreshToken(String refreshToken);
}
