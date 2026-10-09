import java.util.List;

/**
 * Escolhe o pedido que permite fabricar mais discos com o budget atual; no
 * empate, prefere o formato que consome menos PVC por disco.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class EstrategiaMaisDiscos implements EstrategiaProducao {
    /**
     * Escolhe o pedido pendente com maior quantidade financiavel.
     *
     * @param demandas            pedidos conhecidos, em ordem de chegada
     * @param orcamentoDisponivel budget atual, usado para calcular quantas
     *                            unidades de cada pedido sao financiaveis
     * @return o melhor pedido, ou {@code null} se nenhum for financiavel
     */
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        Demanda melhor = null;
        int maiorQuantidade = 0;
        for (Demanda demanda : demandas) {
            if (!demanda.estaPendente()) {
                continue;
            }
            int quantidade = demanda.calcularQuantidadeViavel(orcamentoDisponivel);
            if (quantidade > maiorQuantidade || (quantidade > 0 && quantidade == maiorQuantidade
                    && demanda.getConsumoPorUnidade() < melhor.getConsumoPorUnidade())) {
                melhor = demanda;
                maiorQuantidade = quantidade;
            }
        }
        return melhor;
    }

    /**
     * Retorna o nome da estrategia.
     *
     * @return nome legivel da estrategia
     */
    @Override
    public String getNomeEstrategia() {
        return "Mais discos na loja (maximizar producao)";
    }
}
