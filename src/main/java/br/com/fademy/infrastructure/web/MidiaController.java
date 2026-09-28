package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class MidiaController {

    private final EnviarAvatarUseCase enviarAvatar;
    private final EnderecoDasImagens imagens;

    public MidiaController(EnviarAvatarUseCase enviarAvatar, EnderecoDasImagens imagens) {
        this.enviarAvatar = enviarAvatar;
        this.imagens = imagens;
    }

    /** SPEC RN14. Público: acontece antes de a conta existir. */
    @PostMapping("/api/media/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse enviar(@RequestParam(name = "file", required = false) MultipartFile arquivo)
            throws IOException {
        String chave = arquivo == null
                ? enviarAvatar.executar(null, null)
                : enviarAvatar.executar(arquivo.getBytes(), arquivo.getContentType());
        return new UploadResponse(chave, imagens.url(chave));
    }

    public record UploadResponse(String key, String objectUrl) {
    }
}
