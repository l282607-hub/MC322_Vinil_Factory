/**
 * Maquina de inspecao: ouve o disco em busca de ruidos e examina a superficie.
 *
 * <p>A chance de rejeicao e diretamente proporcional a qualidade do disco
 * (criterios mais rigorosos para discos premium) somada a probabilidade de
 * falha acumulada nas maquinas anteriores. Alem disso, a propria estacao pode
 * falhar conforme sua saude e o cenario, invertendo o resultado.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class EstacaoInspecao extends Maquina {
    /** Peso base da qualidade; o cenario tambem modifica o rigor. */
    private static final double FATOR_RIGOR = 0.25;

    /**
     * Cria uma estacao de inspecao.
     *
     * @param nome             nome da maquina
     * @param capacidadeMaxima maximo de discos por lote
     * @param chanceFalha      chance base de o sensor errar, entre 0 e 1
     * @param custoOperacao    reais por disco inspecionado
     */
    public EstacaoInspecao(String nome, int capacidadeMaxima, double chanceFalha, double custoOperacao) {
        super(nome, capacidadeMaxima, chanceFalha, custoOperacao);
    }

    /**
     * Inspeciona o disco, marcando-o como aprovado ou rejeitado. Se o sensor
     * falhar (sorteio), o resultado da inspecao e invertido.
     *
     * @param produto disco a inspecionar
     * @return {@code true} se o disco foi aprovado; {@code false} se foi
     *         rejeitado ou se a estacao nao operou
     */
    @Override
    public boolean processar(Produto produto) {
        if (!estaLigada()) {
            return false;
        }

        boolean rejeitado = sortear() < calcularChanceRejeicao(produto);

        if (verificarFalha()) {
            rejeitado = !rejeitado;
            System.out.printf("    [!] %s: leitura incorreta do sensor no disco #%03d%n",
                    getNome(), produto.getId());
        }

        produto.setStatus(rejeitado ? StatusProduto.REJEITADO : StatusProduto.APROVADO);
        registrarUso();
        return !rejeitado;
    }

    /** Qualidade alta = criterio rigoroso; defeitos acumulados somam ao risco. */
    private double calcularChanceRejeicao(Produto produto) {
        return Math.min(1.0, produto.getQualidade() * FATOR_RIGOR * getFatorFalhaCenario()
                + produto.getProbabilidadeFalhaAcumulada());
    }

    /**
     * Informa o tipo da maquina.
     *
     * @return "Inspecao"
     */
    @Override
    public String getTipo() {
        return "Inspecao";
    }
}
