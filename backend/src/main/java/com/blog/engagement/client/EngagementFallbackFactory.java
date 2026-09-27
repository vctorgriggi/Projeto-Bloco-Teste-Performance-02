package com.blog.engagement.client;

import com.blog.engagement.client.dto.CommentView;
import com.blog.engagement.client.dto.NewReaction;
import com.blog.engagement.client.dto.ReactionSummaryView;
import com.blog.engagement.client.dto.ServiceInfoView;
import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.InvalidRequestException;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

// o que acontece quando a chamada ao engajamento nao completa.
//
// e uma FallbackFactory, e nao um fallback simples, porque ela recebe a causa da
// falha -- e a causa e justamente o que decide o comportamento. o spring cloud
// circuitbreaker chama o fallback para qualquer excecao, inclusive as de negocio
// que o EngagementErrorDecoder acabou de traduzir; se este fallback engolisse
// tudo, um 404 legitimo do outro servico viraria "servico indisponivel".
//
// por isso a regra e: excecao que ja tem significado passa reto; o resto (conexao
// recusada, timeout, circuito aberto) vira ServiceUnavailableException, que o
// handler global transforma em 503.
public class EngagementFallbackFactory implements FallbackFactory<EngagementClient> {

    private static final Logger log = LoggerFactory.getLogger(EngagementFallbackFactory.class);

    @Override
    public EngagementClient create(Throwable cause) {
        return new EngagementFallback(cause);
    }

    private static class EngagementFallback implements EngagementClient {

        private final Throwable cause;

        private EngagementFallback(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public List<CommentView> listComments(Long postId) {
            throw traduzir();
        }

        @Override
        public void deleteComment(Long commentId) {
            throw traduzir();
        }

        @Override
        public ReactionSummaryView reactionSummary(Long postId, String reader) {
            throw traduzir();
        }

        @Override
        public ReactionSummaryView react(Long postId, NewReaction body) {
            throw traduzir();
        }

        @Override
        public ReactionSummaryView undoReaction(Long postId, String type, String reader) {
            throw traduzir();
        }

        // a unica chamada que degrada em vez de falhar: perguntar se o servico esta
        // de pe e ter como resposta "nao esta" e uma resposta valida, e e ela que
        // permite a interface avisar o leitor antes de ele tentar comentar.
        @Override
        public ServiceInfoView ping() {
            RuntimeException falha = traduzirSePossivel();
            if (falha != null) {
                log.debug("ping ao engagement-service falhou: {}", falha.getMessage());
            }
            return new ServiceInfoView("engagement-service", "DOWN");
        }

        private RuntimeException traduzir() {
            RuntimeException doDominio = traduzirSePossivel();
            if (doDominio != null) {
                return doDominio;
            }
            log.warn("chamada ao engagement-service nao completou: {}", cause.toString());
            return new ServiceUnavailableException(
                    "O servico de engajamento esta indisponivel. Tente novamente em instantes.", cause);
        }

        // procura, na cadeia de causas, uma excecao que o decoder ja tenha traduzido.
        // o circuit breaker pode executar a chamada em outra thread e embrulhar o
        // erro, entao olhar so o primeiro nivel nao basta.
        private RuntimeException traduzirSePossivel() {
            Throwable atual = cause;
            while (atual != null) {
                if (atual instanceof ResourceNotFoundException
                        || atual instanceof BusinessRuleException
                        || atual instanceof InvalidRequestException) {
                    return (RuntimeException) atual;
                }
                atual = atual.getCause() == atual ? null : atual.getCause();
            }
            return null;
        }
    }
}
