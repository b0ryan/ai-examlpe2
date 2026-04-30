package lottery.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lottery.config.AppConfig;

import java.time.Instant;
import java.util.Date;

public class AuthService {
    private final AppConfig config;

    public AuthService(AppConfig config) {
        this.config = config;
    }

    public String issueToken(long userId, String role) {
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("role", role)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(24 * 3600)))
                .sign(Algorithm.HMAC256(config.jwtSecret()));
    }

    public UserContext verify(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(config.jwtSecret())).build().verify(token);
        return new UserContext(Long.parseLong(jwt.getSubject()), jwt.getClaim("role").asString());
    }
}
