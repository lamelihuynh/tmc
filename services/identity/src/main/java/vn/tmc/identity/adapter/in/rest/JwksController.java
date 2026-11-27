package vn.tmc.identity.adapter.in.rest;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.identity.adapter.out.crypto.SigningKeys;
import vn.tmc.platform.security.jwt.AccessTokenContract;

/**
 * Public keys services use to check tokens (RFC 7517). No authentication: they are public. During a
 * key rotation it lists the current and the previous key (C-12).
 */
@RestController
public class JwksController {
  private final String jwks;

  public JwksController(SigningKeys keys) {
    this.jwks = keys.publicJwksJson();
  }

  @GetMapping(value = AccessTokenContract.JWKS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> jwks() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)).cachePublic())
        .body(jwks);
  }
}
