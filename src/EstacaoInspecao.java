public class EstacaoInspecao {
    private boolean ativa;
    private int produtosInspecionados;

    public EstacaoInspecao(boolean ativa) {
        this.ativa = ativa;
    }

    public void ativar() {
        ativa = true;
    }

    public void desativar() {
        ativa = false;
    }

    public boolean inspecionar(Produto produto) {
        if (!ativa) {
            return false;
        }

        produto.aprovar();
        produtosInspecionados++;
        return true;
    }

    public int getTotalInspecionados() {
        return produtosInspecionados;
    }
}
