package vn.tmc.auditadmin.adapter.in.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@Profile("!inmem & !local")
public class DlqMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(DlqMonitor.class);
  private static final String QUEUE = AuditQueueConfig.DLQ;

  private final RabbitTemplate rabbit;
  private final AtomicReference<Double> depth = new AtomicReference<>(Double.NaN);
  private Integer lastObserved;

  public DlqMonitor(RabbitTemplate rabbit, MeterRegistry metrics) {
    this.rabbit = rabbit;
    Gauge.builder("tmc.rabbitmq.dlq.depth", depth, value -> value.get())
        .description("Number of ready messages in the dead-letter queue")
        .tag("queue", QUEUE)
        .register(metrics);
  }

  @Scheduled(
      initialDelayString = "${tmc.dlq.initial-delay-ms:5000}",
      fixedDelayString = "${tmc.dlq.poll-ms:30000}")
  public void refresh() {
    try {
      int count = rabbit.execute(channel -> channel.queueDeclarePassive(QUEUE).getMessageCount());
      depth.set((double) count);
      if (count > 0 && !Integer.valueOf(count).equals(lastObserved)) {
        LOG.warn("Dead-letter queue {} contains {} ready messages", QUEUE, count);
      }
      lastObserved = count;
    } catch (RuntimeException error) {
      depth.set(Double.NaN);
      LOG.warn("Could not read dead-letter queue {}", QUEUE, error);
    }
  }
}
