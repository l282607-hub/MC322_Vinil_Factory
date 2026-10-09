import java.util.List;

/**
 * Estrategia (padrao Strategy) que decide qual pedido produzir a seguir.
 *
 * <p>As implementacoes apenas consultam os pedidos e escolhem um deles: nao
 * alteram estoque, budget nem status. Quem fabrica e o
 * {@link GerenciadorProducao}.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public interface EstrategiaProducao {
    /**
     * Escolhe o proximo pedido a produzir.
     *
     * @param demandas           pedidos conhecidos, em ordem de chegada
     * @param orcamentoDisponivel budget atual, em reais
     * @return o pedido escolhido, ou {@code null} se nenhum for elegivel
     */
    Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel);

    /**
     * Retorna o nome da estrategia para exibicao no cabecalho do menu.
     *
     * @return nome legivel da estrategia
     */
    String getNomeEstrategia();
}
