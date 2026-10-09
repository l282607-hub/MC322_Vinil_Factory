import java.util.List;

/**
 * Respeita a ordem em que as gravadoras enviaram seus pedidos: escolhe o
 * primeiro pedido pendente.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class EstrategiaFilaGravadoras implements EstrategiaProducao {
    /**
     * Escolhe o pedido pendente mais antigo, sem considerar o budget.
     *
     * @param demandas            pedidos conhecidos, em ordem de chegada
     * @param orcamentoDisponivel ignorado por esta estrategia
     * @return o primeiro pedido pendente, ou {@code null} se nao houver
     */
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        for (Demanda demanda : demandas) {
            if (demanda.estaPendente()) {
                return demanda;
            }
        }
        return null;
    }

    /**
     * Retorna o nome da estrategia.
     *
     * @return nome legivel da estrategia
     */
    @Override
    public String getNomeEstrategia() {
        return "Fila das gravadoras (ordem de chegada)";
    }
}
