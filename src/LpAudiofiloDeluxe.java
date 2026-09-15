/**
 * LP duplo de 180g para audiofilos. Maior qualidade (0.9), maior consumo de
 * PVC e inspecao mais rigorosa.
 */
public class LpAudiofiloDeluxe extends Produto {
    public static final String TIPO = "LP Audiofilo Deluxe";
    public static final double PVC_POR_UNIDADE = 0.40; // kg (2 discos de 180g + sobra)
    public static final double QUALIDADE = 0.9;

    public LpAudiofiloDeluxe() {
        super(TIPO, PVC_POR_UNIDADE, QUALIDADE);
    }

    @Override
    public void processar() {
        setStatus("prensado a 180g em prensagem lenta");
    }

    @Override
    public double calcularTempoProducao() {
        return 6.0; // minutos: dois discos, ciclo de prensagem lento
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
