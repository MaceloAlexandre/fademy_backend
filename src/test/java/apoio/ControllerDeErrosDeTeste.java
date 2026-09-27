package apoio;

import br.com.fademy.domain.excecao.AcessoNegadoException;
import br.com.fademy.domain.excecao.ConflitoException;
import br.com.fademy.domain.excecao.DadosInvalidosException;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.excecao.RecursoNaoEncontradoException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller usado só pelo TratadorDeErrosTest. Fica fora de br.com.fademy para não entrar na
 * varredura de componentes dos testes de integração.
 */
@RestController
public class ControllerDeErrosDeTeste {

    @GetMapping("/teste/{caso}")
    String lancar(@PathVariable String caso) {
        String mensagem = "mensagem de " + caso;
        throw switch (caso) {
            case "dados-invalidos" -> new DadosInvalidosException(mensagem);
            case "nao-autenticado" -> new NaoAutenticadoException(mensagem);
            case "acesso-negado" -> new AcessoNegadoException(mensagem);
            case "nao-encontrado" -> new RecursoNaoEncontradoException(mensagem);
            case "conflito" -> new ConflitoException(mensagem);
            default -> new IllegalStateException("detalhe interno que não pode vazar");
        };
    }

    @PostMapping("/teste/corpo")
    Map<String, Object> corpo(@RequestBody Map<String, Object> corpo) {
        return corpo;
    }
}
