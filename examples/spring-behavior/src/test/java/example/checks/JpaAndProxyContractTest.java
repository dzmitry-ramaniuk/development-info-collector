package example.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class JpaAndProxyContractTest {
    @Test
    void crudPagingModifyingAndRollbackFollowDocumentedContracts() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            UserRepository repository = context.getBean(UserRepository.class);
            repository.save(new UserEntity(1L, "before"));
            assertThat(repository.findAll(PageRequest.of(0, 10)).getContent()).hasSize(1);
            assertThat(repository.rename(1L, "committed")).isEqualTo(1);
            RenameService service = context.getBean(RenameService.class);
            assertThatThrownBy(() -> service.renameAndFail(1L)).isInstanceOf(IllegalStateException.class);
            assertThat(repository.findById(1L).orElseThrow().name).isEqualTo("committed");
            assertThat(service.transactionActive()).isTrue();
            assertThat(service.selfInvocation()).isFalse();
        }
    }

    @Entity(name = "ContractUser")
    public static class UserEntity {
        @Id public Long id;
        public String name;
        public UserEntity() { }
        UserEntity(Long id, String name) { this.id = id; this.name = name; }
    }

    public interface UserRepository extends CrudRepository<UserEntity, Long>,
            PagingAndSortingRepository<UserEntity, Long> {
        @Modifying
        @Transactional
        @Query("update ContractUser u set u.name = ?2 where u.id = ?1")
        int rename(Long id, String name);
    }

    public static class RenameService {
        private final UserRepository repository;
        RenameService(UserRepository repository) { this.repository = repository; }
        @Transactional
        public void renameAndFail(Long id) {
            repository.rename(id, "rolled back");
            throw new IllegalStateException("rollback");
        }
        @Transactional(readOnly = true)
        public boolean transactionActive() {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }
        public boolean selfInvocation() { return transactionActive(); }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = JpaAndProxyContractTest.class, considerNestedRepositories = true)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:jpa-contract;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan("example.checks");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
        @Bean RenameService renameService(UserRepository repository) { return new RenameService(repository); }
    }
}
