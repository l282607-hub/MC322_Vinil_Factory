/**
 * Prensa o PVC aquecido. Uma falha deixa bolhas ou empenamento e aumenta
 * o risco do disco; a rejeicao e decidida depois pela inspecao.
 * Uma quebra por desgaste impede novos usos.
 */
public class PrensaHidraulica extends Maquina {
    private static final double INCREMENTO_FALHA = 0.15;

    public PrensaHidraulica(String nome, int capacidadeMaxima, double chanceDefeito, double custoOperacao) {
        super(nome, capacidadeMaxima, chanceDefeito, custoOperacao);
    }

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

    @Override
    public String getTipo() {
        return "Processamento";
    }
}
