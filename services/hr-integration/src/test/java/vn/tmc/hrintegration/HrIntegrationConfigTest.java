package vn.tmc.hrintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.tmc.hrintegration.adapter.out.http.HttpHrPayrollAdapter;
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;
import vn.tmc.hrintegration.bootstrap.HrIntegrationConfig;

class HrIntegrationConfigTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withPropertyValues("spring.profiles.active=inmem")
          .withBean(ObjectMapper.class, () -> new ObjectMapper())
          .withUserConfiguration(HrIntegrationConfig.class);

  @Test
  void should_select_simulated_adapter_by_default() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(IHrPayrollPort.class);
          assertThat(context.getBean(IHrPayrollPort.class))
              .isInstanceOf(SimulatedHrPayrollAdapter.class);
        });
  }

  @Test
  void should_select_http_adapter_when_http_mode_is_enabled() {
    runner
        .withPropertyValues(
            "tmc.hr.mode=http",
            "tmc.hr.url=http://127.0.0.1:8089",
            "HR_PAYROLL_API_TOKEN=test-token")
        .run(
            context -> {
              assertThat(context).hasSingleBean(IHrPayrollPort.class);
              assertThat(context.getBean(IHrPayrollPort.class))
                  .isInstanceOf(HttpHrPayrollAdapter.class);
            });
  }
}