/**
 * Pedido de uma gravadora: quantos discos de um tipo ainda precisam ser
 * fabricados e aprovados.
 */
public class Demanda {
    private String tipoProduto;
    private int quantidadeProdutos;
    private boolean atendida;

    public Demanda(String tipoProduto, int quantidadeProdutos) {
        this.tipoProduto = tipoProduto;
        this.quantidadeProdutos = Math.max(0, quantidadeProdutos);
        this.atendida = this.quantidadeProdutos == 0;
    }

    /** Substitui a quantidade pendente (nova encomenda da gravadora). */
    public void atualizarQuantidade(int novaQuantidade) {
        if (novaQuantidade >= 0) {
            quantidadeProdutos = novaQuantidade;
            atendida = quantidadeProdutos == 0;
        }
    }

    /** Materia-prima total para fabricar a quantidade pendente. */
    public double calcularMateriaPrimaNecessaria(double quantidadePorUnidade) {
        return quantidadeProdutos * quantidadePorUnidade;
    }

    /** Registra discos aprovados; marca como atendida quando zera. */
    public void atender(int quantidadeAprovada) {
        quantidadeProdutos = Math.max(0, quantidadeProdutos - quantidadeAprovada);
        atendida = quantidadeProdutos == 0;
    }

    public String getTipoProduto() {
        return tipoProduto;
    }

    public int getQuantidadeProdutos() {
        return quantidadeProdutos;
    }

    public boolean isAtendida() {
        return atendida;
    }

    @Override
    public String toString() {
        return String.format("%-28s | pendente: %3d | %s",
                tipoProduto, quantidadeProdutos, atendida ? "atendida" : "em aberto");
    }
}
