package learning;

import jakarta.jms.Connection;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.UUID;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcknowledgementTest {
    private BrokerService broker;
    private ActiveMQConnectionFactory factory;

    @BeforeEach
    void startBroker() throws Exception {
        String name = "ack-practice-" + UUID.randomUUID();
        broker = new BrokerService();
        broker.setBrokerName(name);
        broker.setPersistent(false); // Это проверка ack, не disk durability.
        broker.setUseJmx(false);
        broker.start();
        broker.waitUntilStarted();
        factory = new ActiveMQConnectionFactory("vm://" + name + "?create=false");
        factory.getRedeliveryPolicy().setInitialRedeliveryDelay(0);
        factory.getRedeliveryPolicy().setRedeliveryDelay(0);
        factory.getRedeliveryPolicy().setMaximumRedeliveries(1);
    }

    @AfterEach
    void stopBroker() throws Exception {
        if (broker != null) {
            broker.stop();
            broker.waitUntilStopped();
        }
    }

    private void send(Session session, String body) throws Exception {
        try (var producer = session.createProducer(session.createQueue("orders.queue"))) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(session.createTextMessage(body));
        }
    }

    @Test
    void rollbackRedeliversAndCommitAcknowledges() throws Exception {
        try (Connection connection = factory.createConnection();
             Session sender = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             Session receiver = connection.createSession(true, Session.SESSION_TRANSACTED);
             MessageConsumer consumer = receiver.createConsumer(receiver.createQueue("orders.queue"))) {
            connection.start();
            send(sender, "order-42");
            Message first = consumer.receive(2000);
            assertNotNull(first);
            assertFalse(first.getJMSRedelivered());
            receiver.rollback();
            Message repeated = consumer.receive(2000);
            assertNotNull(repeated);
            assertTrue(repeated.getJMSRedelivered());
            assertEquals(2, repeated.getIntProperty("JMSXDeliveryCount"));
            receiver.commit();
            assertNull(consumer.receiveNoWait());
        }
    }

    @Test
    void exhaustedRedeliveryMovesPoisonMessageToDlq() throws Exception {
        try (Connection connection = factory.createConnection();
             Session sender = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             Session receiver = connection.createSession(true, Session.SESSION_TRANSACTED);
             MessageConsumer consumer = receiver.createConsumer(receiver.createQueue("orders.queue"));
             MessageConsumer dlq = sender.createConsumer(sender.createQueue("ActiveMQ.DLQ"))) {
            connection.start();
            send(sender, "poison-42");
            assertNotNull(consumer.receive(2000));
            receiver.rollback();
            assertNotNull(consumer.receive(2000));
            receiver.rollback();
            TextMessage failed = (TextMessage) dlq.receive(2000);
            assertNotNull(failed);
            assertEquals("poison-42", failed.getText());
        }
    }
}
