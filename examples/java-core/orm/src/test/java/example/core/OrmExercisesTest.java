package example.core;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrmExercisesTest {
    @Entity(name = "CoreCustomer")
    @Table(name = "core_customers")
    public static class Customer {
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
        Long id;
        String name;
        @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        List<Purchase> purchases = new ArrayList<>();
        protected Customer() {}
        Customer(String name) { this.name = name; }
        void addPurchase() {
            Purchase purchase = new Purchase();
            purchase.customer = this;
            purchases.add(purchase);
        }
    }

    @Entity(name = "CorePurchase")
    @Table(name = "core_purchases")
    public static class Purchase {
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
        Long id;
        @ManyToOne(fetch = FetchType.LAZY)
        Customer customer;
        protected Purchase() {}
    }

    @Entity(name = "CoreUuidItem")
    @Table(name = "core_uuid_items")
    public static class UuidItem {
        @Id @GeneratedValue(strategy = GenerationType.UUID)
        UUID id;
        protected UuidItem() {}
    }

    private SessionFactory factory() {
        return new Configuration()
            .addAnnotatedClass(Customer.class)
            .addAnnotatedClass(Purchase.class)
            .addAnnotatedClass(UuidItem.class)
            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
            .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
            .setProperty("hibernate.connection.username", "sa")
            .setProperty("hibernate.connection.password", "")
            .setProperty("hibernate.hbm2ddl.auto", "create-drop")
            .setProperty("hibernate.cache.use_second_level_cache", "false")
            .setProperty("hibernate.cache.use_query_cache", "false")
            .setProperty("hibernate.generate_statistics", "true")
            .buildSessionFactory();
    }

    @Test
    void mergeCopiesStateAndFlushDoesNotCommit() {
        try (SessionFactory factory = factory()) {
            Customer original = new Customer("initial");
            factory.inTransaction(session -> session.persist(original));
            original.name = "merged";
            try (var session = factory.openSession()) {
                var transaction = session.beginTransaction();
                try {
                    Customer managed = session.merge(original);
                    assertNotSame(original, managed);
                    assertFalse(session.contains(original));
                    assertTrue(session.contains(managed));
                    assertEquals("merged", managed.name);
                    original.name = "detached only";
                    assertEquals("merged", managed.name);
                    session.flush();
                    transaction.rollback();
                } finally {
                    if (transaction.isActive()) transaction.rollback();
                }
            }
            factory.inTransaction(session ->
                assertEquals("initial", session.find(Customer.class, original.id).name));
        }
    }

    @Test
    void fetchPlanChangesQueryCountWithoutChangingResult() {
        try (SessionFactory factory = factory()) {
            factory.inTransaction(session -> {
                for (int i = 0; i < 3; i++) {
                    Customer customer = new Customer("customer-" + i);
                    customer.addPurchase();
                    customer.addPurchase();
                    session.persist(customer);
                }
            });
            factory.getStatistics().clear();
            factory.inTransaction(session -> {
                List<Customer> customers = session.createQuery(
                    "select c from CoreCustomer c", Customer.class).getResultList();
                assertEquals(3, customers.size());
                assertEquals(6, customers.stream().mapToInt(c -> c.purchases.size()).sum());
            });
            assertEquals(4, factory.getStatistics().getPrepareStatementCount());
            factory.getStatistics().clear();
            factory.inTransaction(session -> {
                List<Customer> customers = session.createQuery(
                    "select distinct c from CoreCustomer c left join fetch c.purchases", Customer.class)
                    .getResultList();
                assertEquals(3, customers.size());
                assertEquals(6, customers.stream().mapToInt(c -> c.purchases.size()).sum());
            });
            assertEquals(1, factory.getStatistics().getPrepareStatementCount());
        }
    }

    @Test
    void standardUuidStrategyAssignsIdentifier() {
        try (SessionFactory factory = factory()) {
            UuidItem item = new UuidItem();
            factory.inTransaction(session -> session.persist(item));
            assertNotNull(item.id);
            factory.inTransaction(session -> assertNotNull(session.find(UuidItem.class, item.id)));
        }
    }
}
