package br.com.fademy.infrastructure.web;

import br.com.fademy.domain.conversa.Mensagem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Mensagem como aparece na API e no evento message:new (SPEC 5.3).
 */
public record MensagemResponse(@JsonProperty("_id") String id, String conversationId, String senderId,
                               @JsonInclude(JsonInclude.Include.NON_NULL) String text, List<String> mediaUrls,
                               Instant createdAt) {

    public static MensagemResponse de(Mensagem m, EnderecoDasImagens imagens) {
        return new MensagemResponse(m.id(), m.conversaId(), m.remetenteId(), m.texto(), imagens.urls(m.midias()),
                m.criadaEm());
    }
}
