/**
 * Parametros da simulacao escolhidos antes de montar a linha de producao.
 *
 * <p>Cada cenario define o budget inicial, quanto as falhas das maquinas sao
 * amplificadas e quanto desgaste cada uso causa.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public enum Cenario {
    /** Estudio em dia: budget alto, falhas e desgaste baixos. */
    IDEAL("Ideal - estudio em dia", 10000.0, 0.1, 0.5),
    /** Turne do caos: budget curto, falhas e desgaste altos. */
    APOCALIPTICO("Apocaliptico - turne do caos", 160.0, 3.0, 12.0);

    private final String descricao;
    private final double budgetInicial;
    private final double fatorFalha;
    private final double desgasteMaximo;

    Cenario(String descricao, double budgetInicial, double fatorFalha, double desgasteMaximo) {
        this.descricao = descricao;
        this.budgetInicial = budgetInicial;
        this.fatorFalha = fatorFalha;
        this.desgasteMaximo = desgasteMaximo;
    }

    /**
     * Retorna o nome do cenario para exibicao.
     *
     * @return descricao curta do cenario
     */
    public String getDescricao() { return descricao; }

    /**
     * Retorna o orcamento com que a fabrica comeca.
     *
     * @return budget inicial em reais
     */
    public double getBudgetInicial() { return budgetInicial; }

    /**
     * Retorna o multiplicador aplicado as chances de falha das maquinas.
     *
     * @return fator de falha (0,1 no cenario ideal, 3 no apocaliptico)
     */
    public double getFatorFalha() { return fatorFalha; }

    /**
     * Retorna o desgaste maximo, em pontos de saude, de um unico uso.
     *
     * @return limite superior (exclusivo) do desgaste sorteado por uso
     */
    public double getDesgasteMaximo() { return desgasteMaximo; }
}
