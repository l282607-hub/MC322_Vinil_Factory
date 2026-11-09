package br.unicamp.ic.mc322;

import br.unicamp.ic.mc322.persistence.PersistenciaPlanta;
import br.unicamp.ic.mc322.service.GerenciadorProducao;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MenuPersistenciaTest {
    @TempDir Path pasta;

    @Test void salvaCarregaESubstituiEstadoPeloMenu() throws Exception {
        Path arquivo = pasta.resolve("estado.xml");
        Path atualizado = pasta.resolve("atualizado.xml");
        String primeira = sessao("1\n4\n2\n7\n2\n1\n2\n0\n5\n3\n8\n" + arquivo + "\n0\n");
        assertTrue(primeira.contains("[OK] Planta salva"));
        assertTrue(primeira.contains("Agulha levantada"));
        String segunda = sessao("2\n9\n" + arquivo + "\n4\n1\n8\n" + atualizado + "\n0\n");
        assertTrue(segunda.contains("[OK] Planta carregada"));
        assertFalse(segunda.contains("FALHA INESPERADA"));
        GerenciadorProducao recuperada = new PersistenciaPlanta().carregar(atualizado);
        assertEquals(9991, recuperada.getBudget());
        assertEquals(23, recuperada.getMateriaPrima().getQuantidade());
        assertEquals(2, recuperada.getLinhas().size());
        assertEquals(2, recuperada.getNumeroLinhaAtual());
        assertEquals(3, recuperada.getDemandas().size());
        assertTrue(recuperada.getNomeEstrategia().startsWith("Mais discos"));
    }

    @Test void falhaAoCarregarMantemEstadoEPermiteContinuar() throws Exception {
        Path corrompido = pasta.resolve("corrompido.xml");
        Path preservado = pasta.resolve("preservado.xml");
        Files.writeString(corrompido, "<planta>");
        String saida = sessao("1\n4\n2\n9\n" + corrompido + "\n\n8\n" + preservado + "\n0\n");
        assertTrue(saida.contains("ERRO DE PERSISTENCIA"));
        assertTrue(saida.contains("A planta atual foi preservada"));
        assertTrue(saida.contains("[OK] Planta salva"));
        GerenciadorProducao recuperada = new PersistenciaPlanta().carregar(preservado);
        assertEquals(9994, recuperada.getBudget());
        assertEquals(22, recuperada.getMateriaPrima().getQuantidade());
    }

    @Test void ausenciaDeArquivoNaoImpedeUsoDaFabrica() throws Exception {
        Path inexistente = pasta.resolve("ausente.xml");
        String saida = sessao("1\n9\n" + inexistente + "\n\n3\n2\n0\n0\n");
        assertTrue(saida.contains("inexistente"));
        assertTrue(saida.contains("Estoque de PVC Reciclado"));
        assertTrue(saida.contains("Agulha levantada"));
        assertFalse(Files.exists(inexistente));
    }

    private static String sessao(String entrada) throws Exception {
        InputStream anterior = System.in;
        PrintStream console = System.out;
        ByteArrayOutputStream capturada = new ByteArrayOutputStream();
        try (PrintStream saida = new PrintStream(capturada, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
            System.setOut(saida);
            Main.main(new String[] {"322"});
            return capturada.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(anterior);
            System.setOut(console);
        }
    }
}
