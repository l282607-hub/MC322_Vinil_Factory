public class Maquina {
    private String nome;
    private boolean ligada;
    private double capacidadeMaxima;

    public Maquina(String nome, boolean ligada, double capacidadeMaxima) {
        this.nome = nome;
        this.ligada = ligada;
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public void ligar() {
        ligada = true;
    }

    public void desligar() {
        ligada = false;
    }

    public boolean processar(MateriaPrima materiaPrima, double demanda) {
        if (!estaLigada()
                || !verificarCapacidade(demanda)
                || !materiaPrima.verificarDisponibilidade(demanda)) {
            return false;
        }

        materiaPrima.consumir(demanda);
        return true;
    }

    public boolean verificarCapacidade(double demanda) {
        return demanda > 0 && demanda <= capacidadeMaxima;
    }

    public String getNome() {
        return nome;
    }

    public boolean estaLigada() {
        return ligada;
    }
}
