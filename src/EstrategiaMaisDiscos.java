import java.util.List;

/** Escolhe a maior quantidade financiavel; no empate, economiza PVC. */
public class EstrategiaMaisDiscos implements EstrategiaProducao {
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

    @Override
    public String getNomeEstrategia() {
        return "Mais discos na loja (maximizar producao)";
    }
}
