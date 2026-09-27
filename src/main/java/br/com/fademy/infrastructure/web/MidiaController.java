package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.application.usecase.EnviarAvatarUseCase.ImagemEnviada;
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

    public MidiaController(EnviarAvatarUseCase enviarAvatar) {
        this.enviarAvatar = enviarAvatar;
    }

    /** SPEC RN14. Público: acontece antes de a conta existir. */
    @PostMapping("/api/media/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse enviar(@RequestParam(name = "file", required = false) MultipartFile arquivo)
            throws IOException {
        ImagemEnviada enviada = arquivo == null
                ? enviarAvatar.executar(null, null)
                : enviarAvatar.executar(arquivo.getBytes(), arquivo.getContentType());
        return new UploadResponse(enviada.chave(), enviada.url());
    }

    public record UploadResponse(String key, String objectUrl) {
    }
}
