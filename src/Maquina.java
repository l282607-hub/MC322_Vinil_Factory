import java.util.Random;

/**
 * Classe abstrata para os equipamentos da linha de producao. Cada maquina
 * processa um Produto por vez, cobra um custo de operacao e pode (dependendo
 * do tipo) afetar a probabilidade de falha do disco ou falhar diretamente.
 */
public abstract class Maquina {
    private static final Random SORTEIO = new Random();

    private String nome;
    private boolean ligada;
    private int capacidadeMaxima;
    private double probabilidadeFalha;
    private double custoOperacao;

    protected Maquina(String nome, int capacidadeMaxima, double probabilidadeFalha, double custoOperacao) {
        this.nome = nome;
        this.ligada = false;
        this.capacidadeMaxima = capacidadeMaxima;
        this.probabilidadeFalha = probabilidadeFalha;
        this.custoOperacao = custoOperacao;
    }

    // ---- Metodos abstratos -------------------------------------------------

    /**
     * Processa um produto.
     * @return true se o produto segue na linha; false se foi rejeitado.
     */
    public abstract boolean processar(Produto produto);

    public abstract String getTipo();

    // ---- Metodos concretos -------------------------------------------------

    public void ligar() {
        ligada = true;
    }

    public void desligar() {
        ligada = false;
    }

    public boolean estaLigada() {
        return ligada;
    }

    public String getNome() {
        return nome;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public double getProbabilidadeFalha() {
        return probabilidadeFalha;
    }

    public double getCustoOperacao() {
        return custoOperacao;
    }

    /** Sorteia, com a probabilidade propria da maquina, se ela falhou nesta operacao. */
    protected boolean verificarFalha() {
        return sortear() < probabilidadeFalha;
    }

    /** Numero aleatorio em [0, 1) compartilhado pelas subclasses. */
    protected double sortear() {
        return SORTEIO.nextDouble();
    }

    @Override
    public String toString() {
        return String.format("%-22s (%s) | falha %.0f%% | R$%.2f/op",
                nome, getTipo(), probabilidadeFalha * 100, custoOperacao);
    }
}
