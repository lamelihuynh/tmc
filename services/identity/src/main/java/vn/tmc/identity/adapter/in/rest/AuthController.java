package vn.tmc.identity.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.identity.application.port.in.IAuthentication;
import vn.tmc.identity.application.port.in.ILogin;
import vn.tmc.platform.security.jwt.AccessTokenContract;

/** Login endpoint (docs/api/identity.openapi.yaml): GRANTED answers with an ES256 access token. */
@RestController
public class AuthController {
  private final ILogin login;

  public AuthController(ILogin login) {
    this.login = login;
  }

  public record LoginRequest(
      @NotBlank String username, @NotBlank String password, String mfaCode) {}

  @PostMapping("/auth/v1/login")
  public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest r) {
    var result = login.login(r.username(), r.password(), r.mfaCode());
    var status =
        switch (result.status()) {
          case GRANTED -> HttpStatus.OK;
          case MFA_REQUIRED -> HttpStatus.UNAUTHORIZED;
          case LOCKED -> HttpStatus.LOCKED;
          case DENIED -> HttpStatus.UNAUTHORIZED;
        };
    if (result.status() != IAuthentication.Status.GRANTED) {
      // Nothing about the account leaks on a refusal: not the subject, not the roles.
      return ResponseEntity.status(status).body(Map.of("status", result.status().name()));
    }
    // subject and roles stay in the body for the dev identity bar of the portal and the employee
    // app, which still send X-Subject / X-Roles on dev profiles.
    var body = new LinkedHashMap<String, Object>();
    body.put("status", result.status().name());
    body.put("subject", result.subject().toString());
    body.put("roles", result.roles().stream().map(Enum::name).sorted().toList());
    body.put("accessToken", result.accessToken());
    body.put("tokenType", "Bearer");
    body.put("expiresIn", AccessTokenContract.TTL.toSeconds());
    body.put("expiresAt", result.expiresAt().toString());
    return ResponseEntity.status(status).body(body);
  }
}
