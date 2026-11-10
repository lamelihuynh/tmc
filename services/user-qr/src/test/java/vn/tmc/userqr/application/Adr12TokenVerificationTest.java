package vn.tmc.userqr.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.userqr.adapter.out.crypto.NimbusTokenCodec;
import vn.tmc.userqr.adapter.out.inmem.InMemoryDeviceKeyRegistry;
import vn.tmc.userqr.adapter.out.inmem.LoggingAuditLog;
import vn.tmc.userqr.application.usecase.DeviceManagementService;
import vn.tmc.userqr.application.usecase.VerifyQrTokenUseCase;
import vn.tmc.userqr.domain.TokenVerificationPolicy;
import vn.tmc.userqr.support.MutableClock;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * ADR-12 compliance tests 1, 2, 6 and 7. Tests 3, 4, 5 (replay) belong to Gate Station, the kiosk
 * and Attendance; test 8 (fraud rule) belongs to Reporting.
 */
class Adr12TokenVerificationTest {
  private final MutableClock clock = new MutableClock();
  private final InMemoryDeviceKeyRegistry registry = new InMemoryDeviceKeyRegistry();
  private final LoggingAuditLog audit = new LoggingAuditLog();
  private final DeviceManagementService management =
      new DeviceManagementService(registry, audit, clock);
  private final VerifyQrTokenUseCase verifier =
      new VerifyQrTokenUseCase(
          new NimbusTokenCodec(), registry, TokenVerificationPolicy.defaults(), clock);

  private final UUID employee = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
  private PhoneSimulator phone;

  private Principal asEmployee(UUID id) {
    return new Principal(id, java.util.Set.of(Role.EMPLOYEE), DataScope.all());
  }

  @BeforeEach
  void enroll() {
    phone = new PhoneSimulator(employee);
    phone.deviceId =
        management.register(asEmployee(employee), employee, phone.publicKeyBase64()).deviceId();
  }

  private String freshToken(String jti) {
    return phone.token(clock.now(), clock.now().plusSeconds(30), jti);
  }

  // test 1
  @Test
  void should_accept_a_token_signed_by_the_registered_device() {
    var result = verifier.verify(freshToken("jti-1"));

    assertThat(result.valid()).isTrue();
    assertThat(result.employeeUuid()).isEqualTo(employee);
    assertThat(result.deviceId()).isEqualTo(phone.deviceId);
    assertThat(result.factories()).containsExactlyInAnyOrder("BD", "LA");
  }

  @Test
  void should_reject_a_token_signed_by_a_different_key_with_the_same_kid() {
    var attacker = new PhoneSimulator(employee);
    var forged =
        PhoneSimulator.sign(
            phone.deviceId,
            employee.toString(),
            clock.now(),
            clock.now().plusSeconds(30),
            "jti-x",
            Map.of(),
            (ECPrivateKey) attacker.keys.getPrivate());

    assertThat(verifier.verify(forged).reason()).isEqualTo(ReasonCode.INVALID_SIGNATURE);
  }

  @Test
  void should_reject_a_device_that_vouches_for_another_employee() {
    var other = UUID.fromString("0192a1c0-0000-7000-8000-000000000002");
    var token =
        PhoneSimulator.sign(
            phone.deviceId,
            other.toString(),
            clock.now(),
            clock.now().plusSeconds(30),
            "jti-y",
            Map.of(),
            (ECPrivateKey) phone.keys.getPrivate());

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.INVALID_SIGNATURE);
  }

  @Test
  void should_reject_an_unknown_device() {
    var stranger = new PhoneSimulator(employee);
    stranger.deviceId = "dev-unknown";

    var result = verifier.verify(stranger.token(clock.now(), clock.now().plusSeconds(30), "jti-z"));

    assertThat(result.reason()).isEqualTo(ReasonCode.DEVICE_UNKNOWN);
  }

  // test 2
  @Test
  void should_reject_tokens_of_a_revoked_device() {
    var token = freshToken("jti-2");
    management.revokeOwn(asEmployee(employee), phone.deviceId);

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.DEVICE_REVOKED);
  }

  @Test
  void should_revoke_the_previous_device_when_a_new_one_is_registered() {
    var oldToken = freshToken("jti-old");
    var newPhone = new PhoneSimulator(employee);
    newPhone.deviceId =
        management.register(asEmployee(employee), employee, newPhone.publicKeyBase64()).deviceId();

    assertThat(verifier.verify(oldToken).reason()).isEqualTo(ReasonCode.DEVICE_REVOKED);
    assertThat(
            verifier
                .verify(newPhone.token(clock.now(), clock.now().plusSeconds(30), "jti-new"))
                .valid())
        .isTrue();
  }

  // test 6
  @Test
  void should_reject_a_token_expired_beyond_the_clock_skew() {
    var token = freshToken("jti-3");
    clock.advance(Duration.ofSeconds(30 + 61));

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.QR_EXPIRED);
  }

  @Test
  void should_accept_a_token_that_expired_within_the_clock_skew() {
    var token = freshToken("jti-4");
    clock.advance(Duration.ofSeconds(30 + 50));

    assertThat(verifier.verify(token).valid()).isTrue();
  }

  @Test
  void should_reject_a_token_issued_in_the_future_beyond_the_skew() {
    var token = phone.token(clock.now().plusSeconds(120), clock.now().plusSeconds(150), "jti-5");

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.CLOCK_SKEW_EXCEEDED);
  }

  @Test
  void should_reject_a_long_lived_token_that_behaves_like_a_static_qr() {
    var token = phone.token(clock.now(), clock.now().plus(Duration.ofHours(24)), "jti-6");

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.MALFORMED_TOKEN);
  }

  // test 7
  @Test
  void should_reject_a_token_that_carries_a_pii_claim() {
    var token =
        phone.token(
            clock.now(), clock.now().plusSeconds(30), "jti-7", Map.of("name", "Nguyen Van A"));

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.MALFORMED_TOKEN);
  }

  @Test
  void should_reject_garbage_and_null_input_without_throwing() {
    assertThat(verifier.verify("not-a-jwt").reason()).isEqualTo(ReasonCode.MALFORMED_TOKEN);
    assertThat(verifier.verify(null).reason()).isEqualTo(ReasonCode.MALFORMED_TOKEN);
    assertThat(verifier.verify("a".repeat(5000)).reason()).isEqualTo(ReasonCode.MALFORMED_TOKEN);
  }

  @Test
  void should_not_let_an_employee_register_a_device_for_someone_else() {
    var other = UUID.fromString("0192a1c0-0000-7000-8000-000000000002");
    var p = new PhoneSimulator(other);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> management.register(asEmployee(employee), other, p.publicKeyBase64()))
        .isInstanceOf(vn.tmc.platform.security.AccessDeniedException.class);
    assertThat(audit.entries()).anyMatch(e -> e.result().equals("DENIED"));
  }

  @Test
  void should_let_hr_staff_revoke_but_not_a_plain_employee() {
    var hr = new Principal(UUID.randomUUID(), java.util.Set.of(Role.HR_STAFF), DataScope.all());
    var token = freshToken("jti-8");

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> management.revokeForEmployee(asEmployee(UUID.randomUUID()), employee))
        .isInstanceOf(vn.tmc.platform.security.AccessDeniedException.class);
    management.revokeForEmployee(hr, employee);

    assertThat(verifier.verify(token).reason()).isEqualTo(ReasonCode.DEVICE_REVOKED);
  }
}
