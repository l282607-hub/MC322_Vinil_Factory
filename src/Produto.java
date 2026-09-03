public class Produto {
    private int id;
    private String nome;
    private String status;
    private double quantidadeMateriaPrimaNecessaria;
    private MateriaPrima materiaPrimaUtilizada;

    public Produto(
            int id,
            String nome,
            String status,
            double quantidadeMateriaPrimaNecessaria) {

        this.id = id;
        this.nome = nome;
        this.status = status;
        this.quantidadeMateriaPrimaNecessaria = quantidadeMateriaPrimaNecessaria;
    }

    public void processar() {
        status = "processado";
    }

    public void aprovar() {
        status = "aprovado";
    }

    public void definirDemandaMateriaPrima(double demanda) {
        if (demanda > 0) {
            quantidadeMateriaPrimaNecessaria = demanda;
        }
    }

    public double getDemandaMateriaPrima() {
        return quantidadeMateriaPrimaNecessaria;
    }

    public void definirMateriaPrimaUtilizada(MateriaPrima materiaPrima) {
        materiaPrimaUtilizada = materiaPrima;
    }

    public MateriaPrima getMateriaPrimaUtilizada() {
        return materiaPrimaUtilizada;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getStatus() {
        return status;
    }
}
