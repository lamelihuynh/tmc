package vn.tmc.attendance.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.attendance.bootstrap.AttendanceApplication;

/** Dev headers work locally, but never rescue a request with an invalid Authorization header. */
@SpringBootTest(classes = AttendanceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"inmem", "test"})
class HrAttendanceDevProfileTest {
  @Autowired MockMvc mvc;

  @Test
  void should_accept_dev_headers_only_without_an_authorization_header() throws Exception {
    var request =
        post("/hr/v1/attempts/{id}/verify-manual", UUID.randomUUID())
            .header("X-Subject", UUID.randomUUID().toString())
            .header("X-Roles", "HR_STAFF")
            .header("X-Scope-Factories", "*")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}");

    // Auth succeeds; the unknown attempt is visible to this global HR caller.
    mvc.perform(request).andExpect(status().isNotFound());
    mvc.perform(request.header("Authorization", "Bearer malformed"))
        .andExpect(status().isForbidden());
  }
}
