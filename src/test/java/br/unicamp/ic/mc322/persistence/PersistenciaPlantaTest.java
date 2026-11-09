package br.unicamp.ic.mc322.persistence;

import br.unicamp.ic.mc322.exception.*;
import br.unicamp.ic.mc322.model.*;
import br.unicamp.ic.mc322.service.GerenciadorProducao;
import br.unicamp.ic.mc322.strategy.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaPlantaTest {
    @TempDir Path pasta;
    private final PersistenciaPlanta persistencia = new PersistenciaPlanta();
    private GerenciadorProducao original;
    private Path arquivo;

    @BeforeEach void preparar() {
        Maquina.definirSemente(322);
        arquivo = pasta.resolve("fabrica.xml");
        original = new GerenciadorProducao(new MateriaPrima(1, "PVC & vinil reciclável", 20, "kg", 3),
                Cenario.IDEAL, new EstrategiaGrandeTiragem());
        original.adicionarMaquina(new PrensaHidraulica("Prensa <A>", 5, 0.2, 4));
        original.adicionarMaquina(new EmbaladoraCapas("Capas", 8, 0.15, 1.5));
        original.adicionarMaquina(new EstacaoInspecao("Ouvido", 6, 0.05, 2.5));
        original.registrarDemanda(CompactoSete.TIPO, 8);
    }

    @Test void recuperaEstadoCompletoAposProducao() {
        original.fabricarDemanda(CompactoSete.TIPO);
        LinhaDeProducao extra = new LinhaDeProducao("Sala B", List.of(
                new PrensaHidraulica("Prensa B", 9, 0.4, 5),
                new EstacaoInspecao("Inspecao B", 7, 0.1, 2)));
        original.adicionarLinha(extra);
        original.selecionarLinha(2);
        original.getLinhas().get(0).getMaquinas().get(0).setLimiarManutencao(80);
        persistencia.salvar(original, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        assertNotSame(original, copia);
        assertEquals(original.getBudget(), copia.getBudget());
        assertEquals(original.getMateriaPrima().toString(), copia.getMateriaPrima().toString());
        assertEquals(original.getNomeEstrategia(), copia.getNomeEstrategia());
        assertEquals(original.getCenario(), copia.getCenario());
        assertEquals(2, copia.getNumeroLinhaAtual());
        assertEquals(original.getUltimoLote(), copia.getUltimoLote());
        assertEquals(original.getDemandas().toString(), copia.getDemandas().toString());
        assertFalse(copia.getProdutosFabricados().isEmpty());
        assertEquals(original.getProdutosFabricados().size(), copia.getProdutosFabricados().size());
        for (int i = 0; i < original.getProdutosFabricados().size(); i++) {
            Produto antes = original.getProdutosFabricados().get(i);
            Produto depois = copia.getProdutosFabricados().get(i);
            assertNotSame(antes, depois);
            assertEquals(antes.getId(), depois.getId());
            assertEquals(antes.getLote(), depois.getLote());
            assertEquals(antes.getTipo(), depois.getTipo());
            assertEquals(antes.getProbabilidadeFalhaAcumulada(), depois.getProbabilidadeFalhaAcumulada());
            assertEquals(StatusProduto.APROVADO, depois.getStatus());
        }
        for (int l = 0; l < original.getLinhas().size(); l++) {
            LinhaDeProducao antes = original.getLinhas().get(l);
            LinhaDeProducao depois = copia.getLinhas().get(l);
            assertEquals(antes.getNome(), depois.getNome());
            assertEquals(antes.getQuantidadeMaquinas(), depois.getQuantidadeMaquinas());
            for (int m = 0; m < antes.getQuantidadeMaquinas(); m++) {
                Maquina a = antes.getMaquinas().get(m);
                Maquina b = depois.getMaquinas().get(m);
                assertNotSame(a, b);
                assertEquals(a.getClass(), b.getClass());
                assertEquals(a.getNome(), b.getNome());
                assertEquals(a.getCapacidadeMaxima(), b.getCapacidadeMaxima());
                assertEquals(a.getCustoOperacao(), b.getCustoOperacao());
                assertEquals(a.getProbabilidadeFalhaBase(), b.getProbabilidadeFalhaBase());
                assertEquals(a.getProbabilidadeFalha(), b.getProbabilidadeFalha(), 1e-14);
                assertEquals(a.getHistoricoFalhas(), b.getHistoricoFalhas());
                assertEquals(a.getLimiarManutencao(), b.getLimiarManutencao());
                assertEquals(a.getSaudeMotor(), b.getSaudeMotor());
                assertEquals(a.getCalibracaoSensor(), b.getCalibracaoSensor());
                assertArrayEquals(a.getIntegridadesAtuadores(), b.getIntegridadesAtuadores());
            }
        }
    }

    @ParameterizedTest @EnumSource(StatusMaquina.class)
    void recuperaComposicaoEEstadoOperacional(StatusMaquina estado) {
        Maquina maquina = original.getLinhas().get(0).getMaquinas().get(0);
        maquina.restaurarEstado(estado, 7, 60, estado == StatusMaquina.QUEBRADA ? 0 : 87.123456789,
                56.3456789, new double[] {99.987654321, 70, 20});
        persistencia.salvar(original, arquivo);
        Maquina recuperada = persistencia.carregar(arquivo).getLinhas().get(0).getMaquinas().get(0);
        assertEquals(estado, recuperada.getStatus());
        assertEquals(estado == StatusMaquina.LIGADA, recuperada.estaLigada());
        assertEquals(maquina.gerarRelatorioDiagnostico(), recuperada.gerarRelatorioDiagnostico());
        assertArrayEquals(maquina.getIntegridadesAtuadores(), recuperada.getIntegridadesAtuadores());
        if (estado == StatusMaquina.QUEBRADA) {
            recuperada.ligar();
            assertFalse(recuperada.estaLigada());
        }
    }

    @ParameterizedTest @EnumSource(StatusDemanda.class)
    void preservaTodosOsStatusDosPedidos(StatusDemanda estado) {
        Demanda demanda = new Demanda(LpStandard.TIPO, estado == StatusDemanda.CONCLUIDA ? 0 : 4,
                estado, LpStandard.PVC_POR_UNIDADE, 8);
        original = GerenciadorProducao.restaurar(original.getMateriaPrima(), 500, Cenario.APOCALIPTICO,
                new EstrategiaMaisDiscos(), original.getLinhas(), 1, List.of(demanda), List.of(), 0);
        persistencia.salvar(original, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        Demanda recuperada = copia.getDemandas().get(0);
        assertEquals(estado, recuperada.getStatus());
        assertEquals(demanda.getQuantidadeProdutos(), recuperada.getQuantidadeProdutos());
        assertEquals(demanda.calcularCustoEstimado(), recuperada.calcularCustoEstimado());
        assertEquals(demanda.calcularConsumoEstimado(), recuperada.calcularConsumoEstimado());
        assertEquals(Cenario.APOCALIPTICO, copia.getCenario());
        assertEquals(original.getNomeEstrategia(), copia.getNomeEstrategia());
    }

    @Test void recuperaFormatosDiferentesESeusIdentificadores() {
        List<Produto> discos = List.of(new CompactoSete(), new LpStandard(), new LpAudiofiloDeluxe());
        for (Produto disco : discos) {
            disco.setLote(2);
            disco.setStatus(StatusProduto.APROVADO);
            disco.aumentarProbabilidadeFalha(0.25);
        }
        original = GerenciadorProducao.restaurar(original.getMateriaPrima(), 500, Cenario.IDEAL,
                new EstrategiaFilaGravadoras(), original.getLinhas(), 1, List.of(), discos, 2);
        persistencia.salvar(original, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        for (int i = 0; i < discos.size(); i++) {
            assertEquals(discos.get(i).getClass(), copia.getProdutosFabricados().get(i).getClass());
            assertEquals(discos.get(i).getId(), copia.getProdutosFabricados().get(i).getId());
            assertEquals(discos.get(i).getQualidade(), copia.getProdutosFabricados().get(i).getQualidade());
        }
        assertTrue(new CompactoSete().getId() > discos.get(2).getId());
    }

    @Test void preservaVinculoExclusivoEPermiteReinstalacao() {
        persistencia.salvar(original, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        LinhaDeProducao origem = copia.getLinhas().get(0);
        LinhaDeProducao destino = new LinhaDeProducao("Destino");
        Maquina maquina = origem.getMaquinas().get(0);
        assertThrows(IllegalArgumentException.class, () -> destino.adicionarMaquina(maquina));
        origem.removerMaquina(maquina);
        destino.adicionarMaquina(maquina);
        assertSame(maquina, destino.getMaquinas().get(0));
        assertEquals(3, original.getLinhas().get(0).getQuantidadeMaquinas());
        double[] valores = maquina.getIntegridadesAtuadores();
        valores[0] = 0;
        assertEquals(100, maquina.getSaude());
        assertThrows(UnsupportedOperationException.class, () -> copia.getDemandas().clear());
    }

    @Test void atualizaArquivoSemAcumularLinhasOuPedidos() {
        persistencia.salvar(original, arquivo);
        original.comprarMateriaPrima(2);
        original.atualizarDemanda(CompactoSete.TIPO, 11);
        persistencia.salvar(original, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        assertEquals(22, copia.getMateriaPrima().getQuantidade());
        assertEquals(9994, copia.getBudget());
        assertEquals(1, copia.getLinhas().size());
        assertEquals(1, copia.getDemandas().size());
        assertEquals(11, copia.getDemandas().get(0).getQuantidadeProdutos());
        assertEquals(1, persistencia.carregar(arquivo).getDemandas().size());
    }

    @Test void continuaProduzindoComRecursosDesgasteELotesRecuperados() {
        original.fabricarDemanda(CompactoSete.TIPO);
        persistencia.salvar(original, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        int maiorId = Produto.getTotalProdutosFabricados();
        Maquina.definirSemente(15);
        original.fabricarDemanda(CompactoSete.TIPO);
        Maquina.definirSemente(15);
        copia.fabricarDemanda(CompactoSete.TIPO);
        assertEquals(2, copia.getUltimoLote());
        assertEquals(original.getBudget(), copia.getBudget());
        assertEquals(original.getMateriaPrima().getQuantidade(), copia.getMateriaPrima().getQuantidade());
        assertEquals(original.getDemandas().toString(), copia.getDemandas().toString());
        assertEquals(original.getProdutosFabricados().size(), copia.getProdutosFabricados().size());
        assertEquals(original.getLinhas().get(0).gerarRelatorioDiagnostico(),
                copia.getLinhas().get(0).gerarRelatorioDiagnostico());
        assertTrue(copia.getProdutosFabricados().stream().filter(p -> p.getLote() == 2)
                .allMatch(p -> p.getId() > maiorId));
    }

    @Test void aceitaPlantaInicialVaziaELinhaEmMontagem() {
        GerenciadorProducao vazia = new GerenciadorProducao(new MateriaPrima(1, "PVC", 0, "kg", 3), 0);
        persistencia.salvar(vazia, arquivo);
        GerenciadorProducao copia = persistencia.carregar(arquivo);
        assertEquals(0, copia.getLinhas().get(0).getQuantidadeMaquinas());
        assertTrue(copia.getDemandas().isEmpty());
        assertTrue(copia.getProdutosFabricados().isEmpty());
        assertThrows(MaquinaIndisponivelException.class, copia::executarProximaProducao);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "<planta>", "texto", "<outra/>",
            "<!DOCTYPE planta [<!ENTITY x SYSTEM 'file:///inexistente'>]><planta>&x;</planta>"})
    void rejeitaArquivosVaziosMalformadosEEntidadesExternas(String conteudo) throws IOException {
        Files.writeString(arquivo, conteudo);
        int contador = Produto.getTotalProdutosFabricados();
        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
        assertEquals(10000, original.getBudget());
        assertEquals(contador, Produto.getTotalProdutosFabricados());
    }

    static Stream<Arguments> valoresInvalidos() {
        return Stream.of(
            Arguments.of("budget=\"10000.0\"", "budget=\"-1\""),
            Arguments.of("budget=\"10000.0\"", "budget=\"NaN\""),
            Arguments.of("quantidade=\"20.0\"", "quantidade=\"-2\""),
            Arguments.of("custo=\"3.0\"", "custo=\"0\""),
            Arguments.of("capacidade=\"5\"", "capacidade=\"0\""),
            Arguments.of("capacidade=\"5\"", "capacidade=\"2147483648\""),
            Arguments.of("falhaBase=\"0.2\"", "falhaBase=\"1.1\""),
            Arguments.of("limiar=\"30.0\"", "limiar=\"101\""),
            Arguments.of("falhas=\"0\"", "falhas=\"-1\""),
            Arguments.of("saude=\"100.0\"", "saude=\"-1\""),
            Arguments.of("saude=\"100.0\"", "saude=\"0\""),
            Arguments.of("calibracao=\"100.0\"", "calibracao=\"Infinity\""),
            Arguments.of("integridade=\"100.0\"", "integridade=\"101\""),
            Arguments.of("numero=\"2\"", "numero=\"1\""),
            Arguments.of("ligado=\"false\"", "ligado=\"true\""),
            Arguments.of("linhaAtiva=\"1\"", "linhaAtiva=\"7\""),
            Arguments.of("status=\"PENDENTE\"", "status=\"CONCLUIDA\""),
            Arguments.of("status=\"PENDENTE\"", "status=\"INVENTADO\""),
            Arguments.of("quantidade=\"8\"", "quantidade=\"0\""),
            Arguments.of("consumo=\"0.1\"", "consumo=\"-0.1\""),
            Arguments.of("consumo=\"0.1\"", "consumo=\"0.4\""),
            Arguments.of("versao=\"1\"", "versao=\"2\""),
            Arguments.of("cenario=\"IDEAL\"", "cenario=\"DESCONHECIDO\""),
            Arguments.of("EstrategiaGrandeTiragem", "OutraEstrategia"),
            Arguments.of("PrensaHidraulica", "OutraMaquina"),
            Arguments.of("Compacto 7 Polegadas", "OutroProduto"),
            Arguments.of("nome=\"PVC &amp; vinil reciclável\"", "nome=\" \""),
            Arguments.of("unidade=\"kg\"", "unidade=\"\""),
            Arguments.of("ultimoLote=\"0\"", "ultimoLote=\"-1\""),
            Arguments.of("totalProdutos=\"", "contador=\"")
        );
    }

    @ParameterizedTest @MethodSource("valoresInvalidos")
    void validaValoresSemAlterarAPlanta(String antes, String depois) throws IOException {
        verificarFalha(xml -> {
            assertTrue(xml.contains(antes), "O caso deve modificar um dado existente: " + antes);
            return xml.replace(antes, depois);
        });
    }

    @Test void rejeitaSecoesDuplicadasDesconhecidasEAusentes() throws IOException {
        verificarFalha(xml -> xml.replace("</planta>", "<linhas/></planta>"));
        verificarFalha(xml -> xml.replace("</planta>", "<desconhecido/></planta>"));
        verificarFalha(xml -> xml.replaceAll("(?s)<materiaPrima[^>]*/>", ""));
        verificarFalha(xml -> xml.replaceAll("(?s)<atuador[^>]*/>", ""));
        verificarFalha(xml -> xml.replace("</linhas>", "<linha nome=\"Sala de vinil\"/></linhas>"));
        verificarFalha(xml -> xml.replace("</demandas>",
                "<demanda tipo=\"Compacto 7 Polegadas\" quantidade=\"1\" status=\"PENDENTE\" consumo=\"0.1\" custo=\"8\"/></demandas>"));
    }

    @Test void falhaTardiaNaoConsomeIdsNemMudaEstado() throws IOException {
        original.fabricarDemanda(CompactoSete.TIPO);
        verificarFalha(xml -> xml.replace("EstrategiaGrandeTiragem", "Invalida"));
        verificarFalha(xml -> xml.replace("ultimoLote=\"1\"", "ultimoLote=\"0\""));
        verificarFalha(xml -> xml.replaceFirst("status=\"APROVADO\"", "status=\"REJEITADO\""));
        verificarFalha(xml -> xml.replaceFirst("risco=\"[^\"]*\"", "risco=\"1.5\""));
        verificarFalha(xml -> xml.replaceFirst("(?s)(<produto[^>]*/>)", "$1$1"));
        verificarFalha(xml -> xml.replaceFirst("totalProdutos=\"[^\"]*\"", "totalProdutos=\"0\""));
    }

    private void verificarFalha(UnaryOperator<String> adulterar) throws IOException {
        persistencia.salvar(original, arquivo);
        String antes = Files.readString(arquivo);
        int contador = Produto.getTotalProdutosFabricados();
        Files.writeString(arquivo, adulterar.apply(antes));
        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
        assertEquals(contador, Produto.getTotalProdutosFabricados());
        Path conferencia = pasta.resolve("conferencia.xml");
        persistencia.salvar(original, conferencia);
        assertEquals(antes, Files.readString(conferencia), "A planta atual deve permanecer intacta");
    }

    @Test void trataAusenciaEFalhasDeLeituraEEscrita() throws IOException {
        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
        assertThrows(PersistenciaException.class, () -> persistencia.carregar(pasta));
        assertThrows(PersistenciaException.class,
                () -> persistencia.salvar(original, pasta.resolve("ausente/fabrica.xml")));
        Path diretorio = Files.createDirectory(pasta.resolve("destino.xml"));
        Files.writeString(diretorio.resolve("preservado.txt"), "dados");
        assertThrows(PersistenciaException.class, () -> persistencia.salvar(original, diretorio));
        assertEquals("dados", Files.readString(diretorio.resolve("preservado.txt")));
        try (Stream<Path> caminhos = Files.list(pasta)) {
            assertFalse(caminhos.anyMatch(p -> p.getFileName().toString().startsWith(".planta-")));
        }
    }

    @Test void salvamentoRejeitadoPreservaArquivoAnterior() throws IOException {
        persistencia.salvar(original, arquivo);
        byte[] antes = Files.readAllBytes(arquivo);
        original.setEstrategia(new EstrategiaProducao() {
            public Demanda selecionarDemanda(List<Demanda> demandas, double budget) { return null; }
            public String getNomeEstrategia() { return "Nao suportada"; }
        });
        assertThrows(PersistenciaException.class, () -> persistencia.salvar(original, arquivo));
        assertArrayEquals(antes, Files.readAllBytes(arquivo));
    }
}
