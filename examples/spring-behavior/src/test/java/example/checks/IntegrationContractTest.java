package example.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.support.TransactionTemplate;

class IntegrationContractTest {
    @Test
    void sharedJdbcDedupIsAtomicRollsBackAndSurvivesNewStoreInstance() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:dedup" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("org/springframework/integration/jdbc/schema-h2.sql")).execute(ds);
        JdbcMetadataStore store = new JdbcMetadataStore(ds);
        store.afterPropertiesSet();
        store.start();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, effects INT NOT NULL)");
        jdbc.update("INSERT INTO orders VALUES (42, 0)");
        TransactionTemplate tx = new TransactionTemplate(new JdbcTransactionManager(ds));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> delivery = () -> {
            start.await();
            tx.executeWithoutResult(status -> {
                if (store.putIfAbsent("orders-consumer:event-1", "processed") == null) {
                    jdbc.update("UPDATE orders SET effects = effects + 1 WHERE id = 42");
                }
            });
            return null;
        };
        try {
            List<Future<Void>> attempts = List.of(pool.submit(delivery), pool.submit(delivery));
            start.countDown();
            for (Future<Void> attempt : attempts) attempt.get(5, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT effects FROM orders WHERE id = 42", Integer.class)).isEqualTo(1);
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                assertThat(store.putIfAbsent("orders-consumer:event-2", "processed")).isNull();
                jdbc.update("UPDATE orders SET effects = effects + 1 WHERE id = 42");
                throw new IllegalStateException("fail before commit");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(store.get("orders-consumer:event-2")).isNull();
            assertThat(jdbc.queryForObject("SELECT effects FROM orders WHERE id = 42", Integer.class)).isEqualTo(1);
            JdbcMetadataStore restarted = new JdbcMetadataStore(ds);
            restarted.afterPropertiesSet();
            restarted.start();
            assertThat(restarted.putIfAbsent("orders-consumer:event-1", "processed")).isEqualTo("processed");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rollbackRestoresJdbcQueueButNotMemoryQueue() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:queue" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("org/springframework/integration/jdbc/schema-h2.sql")).execute(ds);
        var store = new JdbcChannelMessageStore(ds);
        store.setChannelMessageStoreQueryProvider(new H2ChannelMessageStoreQueryProvider());
        store.afterPropertiesSet();
        var durable = new QueueChannel(new MessageGroupQueue(store, "orders"));
        var memory = new QueueChannel(10);
        durable.send(new GenericMessage<>("persistent"));
        memory.send(new GenericMessage<>("memory"));
        var tx = new TransactionTemplate(new JdbcTransactionManager(ds));
        tx.executeWithoutResult(status -> {
            assertThat(durable.receive(0)).isNotNull();
            assertThat(memory.receive(0)).isNotNull();
            status.setRollbackOnly();
        });
        assertThat(durable.receive(0).getPayload()).isEqualTo("persistent");
        assertThat(memory.receive(0)).isNull();
    }
}
