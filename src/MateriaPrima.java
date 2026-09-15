/**
 * Insumo da fabrica. Na GroovePress, o PVC reciclado em kg.
 */
public class MateriaPrima {
    private int id;
    private String nome;
    private double quantidade;
    private String unidade;
    private double custoPorUnidade;

    public MateriaPrima(int id, String nome, double quantidade, String unidade, double custoPorUnidade) {
        this.id = id;
        this.nome = nome;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.custoPorUnidade = custoPorUnidade;
    }

    /** Retira do estoque; retorna false se nao havia quantidade suficiente. */
    public boolean consumir(double consumo) {
        if (!verificarDisponibilidade(consumo)) {
            return false;
        }
        quantidade -= consumo;
        return true;
    }

    public void adicionarEstoque(double quantidadeRecebida) {
        if (quantidadeRecebida > 0) {
            quantidade += quantidadeRecebida;
        }
    }

    public boolean verificarDisponibilidade(double demanda) {
        return demanda > 0 && quantidade >= demanda;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public String getUnidade() {
        return unidade;
    }

    public double getCustoPorUnidade() {
        return custoPorUnidade;
    }

    @Override
    public String toString() {
        return String.format("%s: %.2f %s (R$%.2f/%s)", nome, quantidade, unidade, custoPorUnidade, unidade);
    }
}
