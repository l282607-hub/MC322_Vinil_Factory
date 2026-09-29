import java.util.List;

public interface EstrategiaProducao {
    Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel);
    String getNomeEstrategia();
}
