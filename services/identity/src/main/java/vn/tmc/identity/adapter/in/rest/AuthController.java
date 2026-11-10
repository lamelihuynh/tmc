package vn.tmc.identity.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.identity.application.port.in.IAuthentication;

/**
 * Login endpoint. It answers with the decision only. TODO Team C: on GRANTED return a signed JWT
 * and expose /.well-known/jwks.json. Until then no token exists and nothing downstream trusts this
 * service's answer.
 */
@RestController
public class AuthController {
  private final IAuthentication authentication;

  public AuthController(IAuthentication authentication) {
    this.authentication = authentication;
  }

  public record LoginRequest(
      @NotBlank String username, @NotBlank String password, String mfaCode) {}

  @PostMapping("/auth/v1/login")
  public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest r) {
    var result = authentication.authenticate(r.username(), r.password(), r.mfaCode());
    var status =
        switch (result.status()) {
          case GRANTED -> HttpStatus.OK;
          case MFA_REQUIRED -> HttpStatus.UNAUTHORIZED;
          case LOCKED -> HttpStatus.LOCKED;
          case DENIED -> HttpStatus.UNAUTHORIZED;
        };
    return ResponseEntity.status(status).body(Map.of("status", result.status().name()));
  }
}
