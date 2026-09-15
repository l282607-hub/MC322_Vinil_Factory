/**
 * LP simples de 12 polegadas (140g). Qualidade media (0.7) e consumo medio
 * de PVC.
 */
public class LpStandard extends Produto {
    public static final String TIPO = "LP Standard 12 Polegadas";
    public static final double PVC_POR_UNIDADE = 0.20; // kg
    public static final double QUALIDADE = 0.7;

    public LpStandard() {
        super(TIPO, PVC_POR_UNIDADE, QUALIDADE);
    }

    @Override
    public void processar() {
        setStatus("prensado a 140g em ciclo padrao");
    }

    @Override
    public double calcularTempoProducao() {
        return 3.0; // minutos
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
