package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.VerificarSaudeUseCase;
import br.com.fademy.application.usecase.VerificarSaudeUseCase.Saude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (SPEC RN45).
 */
@RestController
public class SaudeController {

    private final VerificarSaudeUseCase verificarSaude;

    public SaudeController(VerificarSaudeUseCase verificarSaude) {
        this.verificarSaude = verificarSaude;
    }

    @GetMapping("/health")
    public ResponseEntity<SaudeResponse> saude() {
        Saude saude = verificarSaude.executar();
        HttpStatus status = saude.tudoNoAr() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(SaudeResponse.de(saude));
    }

    record SaudeResponse(String status, String postgres, String mongo) {

        static SaudeResponse de(Saude saude) {
            return new SaudeResponse(
                    saude.tudoNoAr() ? "ok" : "degraded",
                    saude.postgresNoAr() ? "up" : "down",
                    saude.mongoNoAr() ? "up" : "down");
        }
    }
}
