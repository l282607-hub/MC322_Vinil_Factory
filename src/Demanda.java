/** Pedido de uma gravadora. A quantidade representa os discos ainda devidos. */
public class Demanda {
    private String tipoProduto;
    private int quantidadeProdutos;
    private StatusDemanda status;
    private double consumoPorUnidade;
    private double custoOperacaoPorUnidade;

    public Demanda(String tipoProduto, int quantidadeProdutos) {
        if (tipoProduto == null || quantidadeProdutos <= 0) {
            throw new IllegalArgumentException("Pedido deve ter tipo e quantidade positiva.");
        }
        this.tipoProduto = tipoProduto;
        this.quantidadeProdutos = quantidadeProdutos;
        this.status = StatusDemanda.PENDENTE;
    }

    public void configurarEstimativas(double consumo, double custo) {
        if (!Double.isFinite(consumo) || !Double.isFinite(custo) || consumo <= 0 || custo <= 0) {
            throw new IllegalArgumentException("Consumo e custo devem ser positivos.");
        }
        consumoPorUnidade = consumo;
        custoOperacaoPorUnidade = custo;
    }

    public void atualizarQuantidade(int quantidade) {
        if (!estaPendente() || quantidade <= 0) {
            throw new IllegalStateException("Somente pedidos pendentes aceitam quantidade positiva.");
        }
        quantidadeProdutos = quantidade;
    }

    public void iniciarProducao() {
        if (!estaPendente()) {
            throw new IllegalStateException("A demanda precisa estar pendente.");
        }
        status = StatusDemanda.EM_PRODUCAO;
    }

    /** Um lote parcial volta para a fila; apenas aprovados abatem o pedido. */
    public void atender(int quantidadeAprovada) {
        if (status != StatusDemanda.EM_PRODUCAO || quantidadeAprovada < 0
                || quantidadeAprovada > quantidadeProdutos) {
            throw new IllegalStateException("Conclusao de lote invalida.");
        }
        quantidadeProdutos -= quantidadeAprovada;
        status = quantidadeProdutos == 0 ? StatusDemanda.CONCLUIDA : StatusDemanda.PENDENTE;
    }

    public void cancelar() {
        if (!estaPendente()) {
            throw new IllegalStateException("Somente uma demanda pendente pode ser cancelada.");
        }
        status = StatusDemanda.CANCELADA;
    }

    public double calcularMateriaPrimaNecessaria(double quantidadePorUnidade) {
        return quantidadeProdutos * quantidadePorUnidade;
    }

    public double calcularConsumoEstimado() {
        return calcularMateriaPrimaNecessaria(consumoPorUnidade);
    }

    public double calcularCustoEstimado() {
        return quantidadeProdutos * custoOperacaoPorUnidade;
    }

    public int calcularQuantidadeViavel(double orcamento) {
        if (!Double.isFinite(orcamento) || orcamento < 0 || custoOperacaoPorUnidade <= 0) {
            return 0;
        }
        return (int) Math.min(quantidadeProdutos, Math.floor((orcamento + 1e-9) / custoOperacaoPorUnidade));
    }

    public boolean ehFinanceiramenteViavel(double orcamento) {
        return calcularQuantidadeViavel(orcamento) == quantidadeProdutos;
    }

    public boolean estaPendente() { return status == StatusDemanda.PENDENTE; }
    public boolean isAtendida() { return status == StatusDemanda.CONCLUIDA; }
    public StatusDemanda getStatus() { return status; }
    public String getTipoProduto() { return tipoProduto; }
    public int getQuantidadeProdutos() { return quantidadeProdutos; }
    public double getConsumoPorUnidade() { return consumoPorUnidade; }

    @Override
    public String toString() {
        return String.format("%-28s | faltam: %3d | %-12s | PVC %.2f kg | operacao R$%.2f",
                tipoProduto, quantidadeProdutos, status.getDescricao(),
                calcularConsumoEstimado(), calcularCustoEstimado());
    }
}
