import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationTest {
    interface Repository { boolean exists(String email); long save(String email); }
    interface Mailer { void welcome(String email); }
    record Registration(Repository repository, Mailer mailer) {
        long register(String email) {
            if (repository.exists(email)) throw new IllegalArgumentException("Duplicate email");
            long id = repository.save(email);
            mailer.welcome(email);
            return id;
        }
    }
    @Mock Repository repository;
    @Mock Mailer mailer;

    @Test void createsUserAndSendsWelcome() {
        when(repository.save("a@example.test")).thenReturn(42L);
        assertEquals(42L, new Registration(repository, mailer).register("a@example.test"));
        verify(mailer).welcome("a@example.test");
    }
    @Test void duplicateDoesNotCreateSideEffects() {
        when(repository.exists("a@example.test")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> new Registration(repository, mailer).register("a@example.test"));
        verify(repository, never()).save(anyString());
        verifyNoInteractions(mailer);
    }
    @Test void failedSaveDoesNotSendWelcome() {
        when(repository.save(anyString())).thenThrow(new IllegalStateException("Database unavailable"));
        assertThrows(IllegalStateException.class, () -> new Registration(repository, mailer).register("a@example.test"));
        verifyNoInteractions(mailer);
    }
}
