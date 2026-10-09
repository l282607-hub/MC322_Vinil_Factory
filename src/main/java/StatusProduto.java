/**
 * Etapa em que um {@link Produto} se encontra na linha de producao.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public enum StatusProduto {
    /** Criado, ainda nao entrou na prensa. */
    AGUARDANDO,
    /** Passou pela prensa hidraulica. */
    PRENSADO,
    /** Passou pela embaladora. */
    EMBALADO,
    /** Passou na inspecao e segue para o armazem. */
    APROVADO,
    /** Reprovado na inspecao; vai para reciclagem. */
    REJEITADO
}
