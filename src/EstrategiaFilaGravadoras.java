import java.util.List;

/** Respeita a ordem em que as gravadoras enviaram seus pedidos. */
public class EstrategiaFilaGravadoras implements EstrategiaProducao {
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        for (Demanda demanda : demandas) {
            if (demanda.estaPendente()) {
                return demanda;
            }
        }
        return null;
    }

    @Override
    public String getNomeEstrategia() {
        return "Fila das gravadoras (ordem de chegada)";
    }
}
