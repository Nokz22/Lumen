package dev.lumen.application.auth;

import dev.lumen.domain.user.EmailAlreadyRegisteredException;
import dev.lumen.domain.user.RefreshToken;
import dev.lumen.domain.user.RefreshTokenRepository;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.UnderageRegistrationException;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MINIMUM_AGE_YEARS = 18;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public AuthTokens register(
            String email,
            String rawPassword,
            String displayName,
            String locale,
            String region,
            LocalDate dateOfBirth) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        int age = Period.between(dateOfBirth, LocalDate.now(ZoneOffset.UTC)).getYears();
        if (age < MINIMUM_AGE_YEARS) {
            throw new UnderageRegistrationException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = userRepository.save(
                new User(email, passwordHash, displayName, locale, region, dateOfBirth, Role.USER));
        return issueTokens(user, UUID.randomUUID(), Instant.now());
    }

    @Transactional
    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user, UUID.randomUUID(), Instant.now());
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(tokenService.hashToken(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();
        if (existing.isRevoked()) {
            // A previously-rotated token was presented again: treat as theft and kill the family.
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            throw new InvalidRefreshTokenException();
        }
        if (existing.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        existing.revoke(now);
        refreshTokenRepository.save(existing);
        return issueTokens(existing.getUser(), existing.getFamilyId(), now);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository
                .findByTokenHash(tokenService.hashToken(rawRefreshToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId()));
    }

    private AuthTokens issueTokens(User user, UUID familyId, Instant now) {
        Instant accessTokenExpiresAt = now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);
        Instant refreshTokenExpiresAt = now.plus(refreshTokenTtlDays, ChronoUnit.DAYS);

        String accessToken = tokenService.generateAccessToken(user, now, accessTokenExpiresAt);
        String rawRefreshToken = tokenService.generateRefreshTokenValue();
        String refreshTokenHash = tokenService.hashToken(rawRefreshToken);

        refreshTokenRepository.save(
                new RefreshToken(user, refreshTokenHash, familyId, now, refreshTokenExpiresAt));

        return new AuthTokens(
                accessToken,
                rawRefreshToken,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                user.getId(),
                user.getRole());
    }
}
