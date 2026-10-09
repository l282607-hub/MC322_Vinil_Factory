import java.util.List;

/**
 * Prioriza a maior tiragem ainda pendente; em caso de empate, vence o pedido
 * que chegou primeiro.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class EstrategiaGrandeTiragem implements EstrategiaProducao {
    /**
     * Escolhe o pedido pendente com mais discos a produzir.
     *
     * @param demandas            pedidos conhecidos, em ordem de chegada
     * @param orcamentoDisponivel ignorado por esta estrategia
     * @return o pedido pendente de maior quantidade, ou {@code null}
     */
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

    /**
     * Retorna o nome da estrategia.
     *
     * @return nome legivel da estrategia
     */
    @Override
    public String getNomeEstrategia() {
        return "Grande tiragem (maior demanda)";
    }
}
