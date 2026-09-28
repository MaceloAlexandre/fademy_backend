package br.com.fademy.infrastructure.web;

import br.com.fademy.application.usecase.EnviarAvatarUseCase;
import br.com.fademy.application.usecase.VerImagemUseCase;
import br.com.fademy.domain.midia.Imagem;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class MidiaController {

    private final EnviarAvatarUseCase enviarAvatar;
    private final VerImagemUseCase verImagem;
    private final EnderecoDasImagens imagens;

    public MidiaController(EnviarAvatarUseCase enviarAvatar, VerImagemUseCase verImagem, EnderecoDasImagens imagens) {
        this.enviarAvatar = enviarAvatar;
        this.verImagem = verImagem;
        this.imagens = imagens;
    }

    @GetMapping("/api/media/{pasta}/{arquivo}")
    public ResponseEntity<byte[]> ver(@PathVariable String pasta, @PathVariable String arquivo) {
        Imagem imagem = verImagem.executar(pasta + "/" + arquivo);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagem.tipo())).body(imagem.conteudo());
    }

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
