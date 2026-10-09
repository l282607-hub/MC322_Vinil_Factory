/**
 * Maquina de processamento: prensa o PVC aquecido. Uma falha deixa bolhas ou
 * empenamento e aumenta o risco do disco; a rejeicao e decidida depois pela
 * inspecao. Uma quebra por desgaste impede novos usos.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class PrensaHidraulica extends Maquina {
    private static final double INCREMENTO_FALHA = 0.15;

    /**
     * Cria uma prensa.
     *
     * @param nome             nome da maquina
     * @param capacidadeMaxima maximo de discos por lote
     * @param chanceDefeito    chance base de deixar defeito no disco, entre 0 e 1
     * @param custoOperacao    reais por disco processado
     */
    public PrensaHidraulica(String nome, int capacidadeMaxima, double chanceDefeito, double custoOperacao) {
        super(nome, capacidadeMaxima, chanceDefeito, custoOperacao);
    }

    /**
     * Prensa o disco; em caso de falha sorteada, soma 0.15 ao risco do disco.
     * A prensa nunca barra o disco: a rejeicao e papel da inspecao.
     *
     * @param produto disco a prensar
     * @return {@code true} se operou; {@code false} se estava desligada ou
     *         quebrada
     */
    @Override
    public boolean processar(Produto produto) {
        if (!estaLigada()) {
            return false;
        }

        produto.processar();

        if (verificarFalha()) {
            produto.aumentarProbabilidadeFalha(INCREMENTO_FALHA);
            System.out.printf("    [!] %s: bolha de ar na prensagem do disco #%03d (+%.0f%% risco)%n",
                    getNome(), produto.getId(), INCREMENTO_FALHA * 100);
        }
        registrarUso();
        return true;
    }

    /**
     * Informa o tipo da maquina.
     *
     * @return "Processamento"
     */
    @Override
    public String getTipo() {
        return "Processamento";
    }
}
