/**
 * Ciclo de vida de um pedido de gravadora.
 *
 * <p>Transicoes validas: {@code PENDENTE -> EM_PRODUCAO}; ao fim do lote,
 * {@code EM_PRODUCAO -> CONCLUIDA} se nada falta ou {@code EM_PRODUCAO ->
 * PENDENTE} se ainda faltam discos; {@code PENDENTE -> CANCELADA} quando
 * faltam recursos. {@code CONCLUIDA} e {@code CANCELADA} sao finais.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see Demanda
 */
public enum StatusDemanda {
    /** Aguardando producao; ainda faltam discos. */
    PENDENTE("Pendente"),
    /** Um lote esta sendo fabricado agora. */
    EM_PRODUCAO("Em producao"),
    /** Todos os discos pedidos foram aprovados. */
    CONCLUIDA("Concluida"),
    /** Encerrado por falta de recursos; nao pode ser reativado. */
    CANCELADA("Cancelada");

    private final String descricao;

    StatusDemanda(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna o nome do status para exibicao.
     *
     * @return descricao em texto simples
     */
    public String getDescricao() {
        return descricao;
    }
}
