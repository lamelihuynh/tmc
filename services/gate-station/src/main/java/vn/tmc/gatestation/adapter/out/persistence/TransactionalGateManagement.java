package vn.tmc.gatestation.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tmc.gatestation.application.port.in.IGateManagement;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.platform.security.Principal;

/** Keeps a gate mutation and its AuditRecorded outbox row in one Gate Store transaction. */
public class TransactionalGateManagement implements IGateManagement {
  private final IGateManagement delegate;
  private final TransactionTemplate transactions;

  public TransactionalGateManagement(IGateManagement delegate, TransactionTemplate transactions) {
    this.delegate = delegate;
    this.transactions = transactions;
  }

  @Override
  public List<GateConfig> list(Principal caller) {
    return delegate.list(caller);
  }

  @Override
  public Optional<GateConfig> find(Principal caller, String gateId) {
    return delegate.find(caller, gateId);
  }

  @Override
  public GateConfig upsert(Principal caller, GateConfig gate) {
    return transactions.execute(status -> delegate.upsert(caller, gate));
  }

  @Override
  public Optional<GateConfig> setMaintenance(Principal caller, String gateId, boolean maintenance) {
    return transactions.execute(status -> delegate.setMaintenance(caller, gateId, maintenance));
  }
}
