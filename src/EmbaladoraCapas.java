/**
 * Maquina de embalagem: coloca o disco no envelope antiestatico e na capa de
 * papelao. Uma falha risca o disco ao envelopar, aumentando seu risco.
 * Uma quebra por desgaste impede novos usos.
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

        produto.setStatus(StatusProduto.EMBALADO);

        if (verificarFalha()) {
            produto.aumentarProbabilidadeFalha(INCREMENTO_FALHA);
            System.out.printf("    [!] %s: risco superficial ao envelopar o disco #%03d (+%.0f%% risco)%n",
                    getNome(), produto.getId(), INCREMENTO_FALHA * 100);
        }
        registrarUso();
        return true;
    }

    @Override
    public String getTipo() {
        return "Embalagem";
    }
}
