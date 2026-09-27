package com.blog.shared.messaging;

// os nomes da topologia no rabbitmq, do ponto de vista do monolito.
//
// o engagement-service tem uma classe com os mesmos nomes, e a repeticao e deliberada,
// pelo mesmo motivo do ApiError: uma biblioteca comum acoplaria os dois servicos em
// tempo de build. o que os une e um contrato, nao um jar -- e o contrato esta escrito em
// docs/EVENTOS.md. se um lado declarar um exchange ou uma fila com argumentos diferentes
// do outro, o broker recusa a declaracao (PRECONDITION_FAILED) e o servico nao sobe, o
// que e o jeito certo de uma divergencia aparecer: alto e cedo.
public final class Topology {

    private Topology() {
    }

    // ---- exchanges -------------------------------------------------------------------

    // fatos sobre posts, publicados por este servico. topic: cada assinante escolhe por
    // routing key o que quer ouvir, e quem publica nao sabe quem assina.
    public static final String POSTS_EXCHANGE = "blog.posts";

    // fatos sobre engajamento, publicados pelo engagement-service
    public static final String ENGAGEMENT_EXCHANGE = "blog.engagement";

    // comandos: mensagens com destinatario. direct, porque um comando tem exatamente uma
    // fila de destino
    public static final String COMMANDS_EXCHANGE = "blog.commands";

    // para onde vai um evento que nenhuma fila assinou (alternate exchange dos topics)
    public static final String UNROUTED_EXCHANGE = "blog.unrouted";
    public static final String UNROUTED_QUEUE = "blog.unrouted";

    // para onde vai uma mensagem que o consumidor recusou de vez (dead letter)
    public static final String DEAD_LETTER_EXCHANGE = "blog.dlx";

    // ---- routing keys ----------------------------------------------------------------

    // o que este servico publica
    public static final String POST_DELETED = "post.deleted";
    public static final String COMMENT_REGISTER = "comment.register";
    public static final String SNAPSHOT_REQUEST = "engagement.snapshot.request";

    // o que este servico assina em blog.engagement: todas as mudancas de engajamento,
    // porque a copia local dos contadores precisa de todas
    public static final String[] ENGAGEMENT_CHANGES = {"comment.*", "reaction.*", "engagement.*"};

    // ---- filas -----------------------------------------------------------------------

    // a fila deste servico: as mudancas de engajamento que alimentam os contadores
    public static final String ENGAGEMENT_SNAPSHOTS_QUEUE = "blog-api.engagement-snapshots";

    // as caixas de entrada do engagement-service para os comandos que este servico envia.
    // sao declaradas aqui tambem (veja MessagingConfig): um comando tem destinatario, e
    // quem o envia garante que a caixa de entrada exista antes de colocar algo nela.
    public static final String COMMENT_COMMANDS_QUEUE = "engagement.comment-commands";
    public static final String SNAPSHOT_REQUESTS_QUEUE = "engagement.snapshot-requests";

    public static String deadLetterQueueOf(String queue) {
        return queue + ".dlq";
    }
}
