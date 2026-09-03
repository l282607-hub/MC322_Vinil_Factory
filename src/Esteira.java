public class Esteira {
    private Object item;
    private boolean emMovimento;
    private double capacidadeMaxima;

    public Esteira(boolean emMovimento, double capacidadeMaxima) {
        this.emMovimento = emMovimento;
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public void ligar() {
        emMovimento = true;
    }

    public void desligar() {
        emMovimento = false;
    }

    public boolean adicionarItem(Object novoItem, double cargaKg) {
        if (!emMovimento || item != null || !verificarCapacidade(cargaKg)) {
            return false;
        }

        item = novoItem;
        return true;
    }

    public Object removerItem() {
        if (!emMovimento) {
            return null;
        }

        Object itemRemovido = item;
        item = null;
        return itemRemovido;
    }

    public boolean verificarCapacidade(double cargaKg) {
        return cargaKg > 0 && cargaKg <= capacidadeMaxima;
    }

    public boolean isEmMovimento() {
        return emMovimento;
    }
}
