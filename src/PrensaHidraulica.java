/**
 * Maquina de processamento: prensa o PVC aquecido no disco. Nao falha
 * diretamente, mas em X% das prensagens deixa micro-bolhas ou empenamento,
 * aumentando a probabilidade de falha acumulada do disco.
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
        return true;
    }

    @Override
    public String getTipo() {
        return "Processamento";
    }
}
