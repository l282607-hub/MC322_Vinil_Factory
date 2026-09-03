public class MateriaPrima {
    private int id;
    private String nome;
    private double quantidadeDisponivel;
    private String unidade;
    private double quantidadeMinima;

    public MateriaPrima(
            int id,
            String nome,
            double quantidadeDisponivel,
            String unidade,
            double quantidadeMinima) {

        this.id = id;
        this.nome = nome;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.unidade = unidade;
        this.quantidadeMinima = quantidadeMinima;
    }

    public void consumir(double consumo) {
        if (verificarDisponibilidade(consumo)) {
            quantidadeDisponivel -= consumo;
        }
    }

    public void adicionarEstoque(double quantidadeRecebida) {
        if (quantidadeRecebida > 0) {
            quantidadeDisponivel += quantidadeRecebida;
        }
    }

    public boolean verificarDisponibilidade(double demanda) {
        return demanda >= quantidadeMinima && quantidadeDisponivel >= demanda;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getQuantidade() {
        return quantidadeDisponivel;
    }

    public String getUnidade() {
        return unidade;
    }
}
