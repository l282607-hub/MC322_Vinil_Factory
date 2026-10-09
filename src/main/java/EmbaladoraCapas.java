/**
 * Maquina de embalagem: coloca o disco no envelope antiestatico e na capa de
 * papelao. Uma falha risca o disco ao envelopar, aumentando seu risco.
 * Uma quebra por desgaste impede novos usos.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class EmbaladoraCapas extends Maquina {
    private static final double INCREMENTO_FALHA = 0.10;

    /**
     * Cria uma embaladora.
     *
     * @param nome             nome da maquina
     * @param capacidadeMaxima maximo de discos por lote
     * @param chanceDefeito    chance base de riscar o disco, entre 0 e 1
     * @param custoOperacao    reais por disco processado
     */
    public EmbaladoraCapas(String nome, int capacidadeMaxima, double chanceDefeito, double custoOperacao) {
        super(nome, capacidadeMaxima, chanceDefeito, custoOperacao);
    }

    /**
     * Embala o disco; em caso de falha sorteada, soma 0.10 ao risco do disco.
     * A embaladora nunca barra o disco.
     *
     * @param produto disco a embalar
     * @return {@code true} se operou; {@code false} se estava desligada ou
     *         quebrada
     */
    @Override
    public boolean processar(Produto produto) {
        if (!estaLigada()) {
            return false;
        }

        produto.setStatus(StatusProduto.EMBALADO);

        if (verificarFalha()) {
            produto.aumentarProbabilidadeFalha(INCREMENTO_FALHA);
            System.out.printf("    [!] %s: risco superficial ao envelopar o disco #%03d (+%.0f%% risco)%n",
                    getNome(), produto.getId(), INCREMENTO_FALHA * 100);
        }
        registrarUso();
        return true;
    }

    /**
     * Informa o tipo da maquina.
     *
     * @return "Embalagem"
     */
    @Override
    public String getTipo() {
        return "Embalagem";
    }
}
