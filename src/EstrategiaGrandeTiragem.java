import java.util.List;

/** Prioriza a maior tiragem ainda pendente; empates respeitam a fila. */
public class EstrategiaGrandeTiragem implements EstrategiaProducao {
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        Demanda maior = null;
        for (Demanda demanda : demandas) {
            if (demanda.estaPendente() && (maior == null
                    || demanda.getQuantidadeProdutos() > maior.getQuantidadeProdutos())) {
                maior = demanda;
            }
        }
        return maior;
    }

    @Override
    public String getNomeEstrategia() {
        return "Grande tiragem (maior demanda)";
    }
}
