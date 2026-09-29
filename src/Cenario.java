/** Parametros da simulacao escolhidos antes de montar a linha. */
public enum Cenario {
    IDEAL("Ideal - estudio em dia", 10000.0, 0.1, 0.5),
    APOCALIPTICO("Apocaliptico - turne do caos", 160.0, 3.0, 12.0);

    private final String descricao;
    private final double budgetInicial;
    private final double fatorFalha;
    private final double desgasteMaximo;

    Cenario(String descricao, double budgetInicial, double fatorFalha, double desgasteMaximo) {
        this.descricao = descricao;
        this.budgetInicial = budgetInicial;
        this.fatorFalha = fatorFalha;
        this.desgasteMaximo = desgasteMaximo;
    }

    public String getDescricao() { return descricao; }
    public double getBudgetInicial() { return budgetInicial; }
    public double getFatorFalha() { return fatorFalha; }
    public double getDesgasteMaximo() { return desgasteMaximo; }
}
