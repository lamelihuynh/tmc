package vn.tmc.gatestation.application.port.out;

import java.util.Collection;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Driven port: the edge copy of employee eligibility, refreshed from the sync bundle (C-02). The
 * last value received for an employee replaces the previous one; nobody is deleted (a leaver
 * arrives as inactive).
 */
public interface IEmployeeDirectoryCache extends IEmployeeDirectory {
  void upsertAll(Collection<EmployeeAccess> employees);
}
