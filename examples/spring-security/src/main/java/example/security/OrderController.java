package example.security;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrderController {
    @GetMapping("/api/orders/{id}")
    Map<String, Long> findById(@PathVariable long id) {
        return Map.of("id", id);
    }

    @GetMapping("/api/admin/ping")
    String adminPing() {
        return "ok";
    }
}
