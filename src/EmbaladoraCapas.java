/**
 * Maquina de embalagem: coloca o disco no envelope antiestatico e na capa de
 * papelao. Nao falha diretamente, mas em Y% dos casos risca o disco ao
 * envelopar, aumentando a probabilidade de falha acumulada.
 */
public class EmbaladoraCapas extends Maquina {
    private static final double INCREMENTO_FALHA = 0.10;

    public EmbaladoraCapas(String nome, int capacidadeMaxima, double chanceDefeito, double custoOperacao) {
        super(nome, capacidadeMaxima, chanceDefeito, custoOperacao);
    }

    @Override
    public boolean processar(Produto produto) {
        if (!estaLigada()) {
            return false;
        }

        produto.setStatus("embalado em capa e envelope antiestatico");

        if (verificarFalha()) {
            produto.aumentarProbabilidadeFalha(INCREMENTO_FALHA);
            System.out.printf("    [!] %s: risco superficial ao envelopar o disco #%03d (+%.0f%% risco)%n",
                    getNome(), produto.getId(), INCREMENTO_FALHA * 100);
        }
        return true;
    }

    @Override
    public String getTipo() {
        return "Embalagem";
    }
}
