/**
 * Compacto de 7 polegadas (single). Qualidade basica (0.5), menor consumo de
 * PVC e inspecao mais flexivel.
 */
public class CompactoSete extends Produto {
    public static final String TIPO = "Compacto 7 Polegadas";
    public static final double PVC_POR_UNIDADE = 0.10; // kg
    public static final double QUALIDADE = 0.5;

    public CompactoSete() {
        super(TIPO, PVC_POR_UNIDADE, QUALIDADE);
    }

    @Override
    public void processar() {
        setStatus("prensado em ciclo rapido");
    }

    @Override
    public double calcularTempoProducao() {
        return 1.5; // minutos
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
