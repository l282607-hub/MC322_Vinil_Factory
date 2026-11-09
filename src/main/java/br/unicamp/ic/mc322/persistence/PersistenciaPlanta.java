package br.unicamp.ic.mc322.persistence;

import br.unicamp.ic.mc322.exception.PersistenciaException;
import br.unicamp.ic.mc322.exception.PlantaException;
import br.unicamp.ic.mc322.model.*;
import br.unicamp.ic.mc322.service.GerenciadorProducao;
import br.unicamp.ic.mc322.strategy.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Salva e recupera a planta em XML usando DOM da biblioteca padrao.
 * O arquivo e hierarquico: planta, linhas, maquinas e suas partes exclusivas.
 * A leitura cria objetos novos; nenhuma referencia da planta atual e recebida.
 */
public class PersistenciaPlanta {
    /** Cria o servico de leitura e escrita XML. */
    public PersistenciaPlanta() { }

    /**
     * Salva um retrato completo. Uma nova gravacao substitui o arquivo anterior.
     * Escreve primeiro num temporario e so entao faz a substituicao atomica.
     * @param planta planta a salvar
     * @param arquivo destino XML, com diretorio pai existente
     * @throws PersistenciaException se nao for possivel validar ou gravar
     */
    public void salvar(GerenciadorProducao planta, Path arquivo) {
        Path temporario = null;
        try {
            if (planta == null || arquivo == null) { throw new IllegalArgumentException("Planta e caminho obrigatorios."); }
            Document documento = novoLeitor().newDocument();
            Element raiz = documento.createElement("planta");
            documento.appendChild(raiz);
            atributos(raiz, "versao", 1, "budget", planta.getBudget(), "cenario", planta.getCenario(),
                    "estrategia", planta.getEstrategiaAtual().getClass().getSimpleName(),
                    "linhaAtiva", planta.getNumeroLinhaAtual(), "ultimoLote", planta.getUltimoLote(),
                    "totalProdutos", Produto.getTotalProdutosFabricados());
            MateriaPrima pvc = planta.getMateriaPrima();
            Element estoque = filho(raiz, "materiaPrima");
            atributos(estoque, "id", pvc.getId(), "nome", pvc.getNome(), "quantidade", pvc.getQuantidade(),
                    "unidade", pvc.getUnidade(), "custo", pvc.getCustoPorUnidade());
            Element linhas = filho(raiz, "linhas");
            for (LinhaDeProducao linha : planta.getLinhas()) {
                Element setor = filho(linhas, "linha");
                atributos(setor, "nome", linha.getNome());
                for (Maquina maquina : linha.getMaquinas()) { escreverMaquina(setor, maquina); }
            }
            Element demandas = filho(raiz, "demandas");
            for (Demanda demanda : planta.getDemandas()) {
                Element pedido = filho(demandas, "demanda");
                atributos(pedido, "tipo", demanda.getTipoProduto(), "quantidade", demanda.getQuantidadeProdutos(),
                        "status", demanda.getStatus(), "consumo", demanda.getConsumoPorUnidade(),
                        "custo", demanda.getCustoOperacaoPorUnidade());
            }
            Element armazem = filho(raiz, "armazem");
            for (Produto produto : planta.getProdutosFabricados()) {
                Element disco = filho(armazem, "produto");
                atributos(disco, "id", produto.getId(), "tipo", produto.getTipo(), "lote", produto.getLote(),
                        "status", produto.getStatus(), "risco", produto.getProbabilidadeFalhaAcumulada());
            }
            // Usa as mesmas regras da leitura antes de substituir um arquivo valido.
            reconstruir(documento);
            Path destino = arquivo.toAbsolutePath();
            temporario = Files.createTempFile(destino.getParent(), ".planta-", ".xml");
            TransformerFactory fabrica = TransformerFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformador = fabrica.newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformador.setOutputProperty(OutputKeys.INDENT, "yes");
            try (OutputStream saida = Files.newOutputStream(temporario)) {
                transformador.transform(new DOMSource(documento), new StreamResult(saida));
            }
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | ParserConfigurationException | TransformerException
                | IllegalArgumentException | PlantaException | SecurityException e) {
            throw new PersistenciaException("Nao foi possivel salvar a planta: " + explicar(e), e);
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); }
                catch (IOException | SecurityException e) {
                    System.err.println("Nao foi possivel remover o temporario: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Valida o documento inteiro e reconstroi objetos e relacionamentos.
     * Nao modifica a fabrica existente nem consome IDs ao encontrar dados invalidos.
     * @param arquivo origem XML
     * @return nova planta, pronta para substituir a atual
     * @throws PersistenciaException se o arquivo estiver ausente, ilegivel ou invalido
     */
    public GerenciadorProducao carregar(Path arquivo) {
        try {
            if (arquivo == null) { throw new IllegalArgumentException("Caminho obrigatorio."); }
            Document documento;
            try (InputStream entrada = Files.newInputStream(arquivo)) {
                documento = novoLeitor().parse(entrada);
            }
            GerenciadorProducao nova = reconstruir(documento);
            Produto.reservarIdentificadores(inteiro(documento.getDocumentElement(), "totalProdutos"));
            return nova;
        } catch (IOException | SAXException | ParserConfigurationException
                | IllegalArgumentException | PlantaException | SecurityException e) {
            throw new PersistenciaException("Nao foi possivel carregar a planta: " + explicar(e), e);
        }
    }

    private static DocumentBuilder novoLeitor() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        DocumentBuilder leitor = fabrica.newDocumentBuilder();
        leitor.setErrorHandler(new DefaultHandler() {
            @Override public void error(SAXParseException e) throws SAXException { throw e; }
            @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
        });
        return leitor;
    }

    private static void escreverMaquina(Element setor, Maquina maquina) {
        Element equipamento = filho(setor, "maquina");
        atributos(equipamento, "tipo", maquina.getClass().getSimpleName(), "nome", maquina.getNome(),
                "capacidade", maquina.getCapacidadeMaxima(), "falhaBase", maquina.getProbabilidadeFalhaBase(),
                "custo", maquina.getCustoOperacao(), "status", maquina.getStatus(),
                "falhas", maquina.getHistoricoFalhas(), "limiar", maquina.getLimiarManutencao());
        atributos(filho(equipamento, "motor"), "saude", maquina.getSaudeMotor(), "ligado", maquina.estaLigada());
        atributos(filho(equipamento, "sensor"), "calibracao", maquina.getCalibracaoSensor());
        Element mecanismos = filho(equipamento, "atuadores");
        double[] valores = maquina.getIntegridadesAtuadores();
        for (int i = 0; i < valores.length; i++) {
            atributos(filho(mecanismos, "atuador"), "numero", i + 1, "integridade", valores[i]);
        }
    }

    private static GerenciadorProducao reconstruir(Document documento) {
        Element raiz = documento.getDocumentElement();
        exigir(raiz != null && raiz.getTagName().equals("planta"), "Elemento raiz deve ser planta.");
        estrutura(raiz, "versao budget cenario estrategia linhaAtiva ultimoLote totalProdutos",
                "materiaPrima linhas demandas armazem");
        exigir(inteiro(raiz, "versao") == 1, "Versao XML nao suportada.");
        int total = inteiro(raiz, "totalProdutos");
        exigir(total >= 0, "Contador de produtos negativo.");
        Element estoque = unico(raiz, "materiaPrima");
        estrutura(estoque, "id nome quantidade unidade custo", "");
        exigir(inteiro(estoque, "id") > 0, "ID de materia-prima deve ser positivo.");
        MateriaPrima pvc = new MateriaPrima(inteiro(estoque, "id"), texto(estoque, "nome"),
                numero(estoque, "quantidade"), texto(estoque, "unidade"), numero(estoque, "custo"));
        Element setores = unico(raiz, "linhas");
        estrutura(setores, "", "linha");
        List<LinhaDeProducao> linhas = new ArrayList<>();
        for (Element setor : filhos(setores, "linha")) {
            estrutura(setor, "nome", "maquina");
            LinhaDeProducao linha = new LinhaDeProducao(texto(setor, "nome"));
            for (Element equipamento : filhos(setor, "maquina")) { lerMaquina(equipamento, linha); }
            linhas.add(linha);
        }
        Element pedidos = unico(raiz, "demandas");
        estrutura(pedidos, "", "demanda");
        List<Demanda> demandas = new ArrayList<>();
        for (Element pedido : filhos(pedidos, "demanda")) {
            estrutura(pedido, "tipo quantidade status consumo custo", "");
            demandas.add(new Demanda(texto(pedido, "tipo"), inteiro(pedido, "quantidade"),
                    StatusDemanda.valueOf(texto(pedido, "status")), numero(pedido, "consumo"), numero(pedido, "custo")));
        }
        Element deposito = unico(raiz, "armazem");
        estrutura(deposito, "", "produto");
        List<Produto> produtos = new ArrayList<>();
        for (Element disco : filhos(deposito, "produto")) {
            estrutura(disco, "id tipo lote status risco", "");
            exigir(texto(disco, "status").equals("APROVADO"), "Armazem aceita somente aprovados.");
            int id = inteiro(disco, "id");
            exigir(id <= total, "ID do disco excede contador salvo.");
            produtos.add(lerProduto(texto(disco, "tipo"), id, inteiro(disco, "lote"), numero(disco, "risco")));
        }
        return GerenciadorProducao.restaurar(pvc, numero(raiz, "budget"),
                Cenario.valueOf(texto(raiz, "cenario")), lerEstrategia(texto(raiz, "estrategia")),
                linhas, inteiro(raiz, "linhaAtiva"), demandas, produtos, inteiro(raiz, "ultimoLote"));
    }

    private static void lerMaquina(Element equipamento, LinhaDeProducao linha) {
        estrutura(equipamento, "tipo nome capacidade falhaBase custo status falhas limiar", "motor sensor atuadores");
        String nome = texto(equipamento, "nome");
        int capacidade = inteiro(equipamento, "capacidade");
        double falha = numero(equipamento, "falhaBase");
        double custo = numero(equipamento, "custo");
        Maquina maquina;
        switch (texto(equipamento, "tipo")) {
            case "PrensaHidraulica": maquina = new PrensaHidraulica(nome, capacidade, falha, custo); break;
            case "EmbaladoraCapas": maquina = new EmbaladoraCapas(nome, capacidade, falha, custo); break;
            case "EstacaoInspecao": maquina = new EstacaoInspecao(nome, capacidade, falha, custo); break;
            default: throw new IllegalArgumentException("Tipo de maquina desconhecido.");
        }
        Element motor = unico(equipamento, "motor");
        estrutura(motor, "saude ligado", "");
        Element sensor = unico(equipamento, "sensor");
        estrutura(sensor, "calibracao", "");
        Element mecanismos = unico(equipamento, "atuadores");
        estrutura(mecanismos, "", "atuador");
        List<Element> atuadores = filhos(mecanismos, "atuador");
        double[] integridades = new double[atuadores.size()];
        for (int i = 0; i < integridades.length; i++) {
            Element atuador = atuadores.get(i);
            estrutura(atuador, "numero integridade", "");
            exigir(inteiro(atuador, "numero") == i + 1, "Ordem ou numero de atuador inconsistente.");
            integridades[i] = numero(atuador, "integridade");
        }
        StatusMaquina status = StatusMaquina.valueOf(texto(equipamento, "status"));
        exigir(texto(motor, "ligado").equals(Boolean.toString(status == StatusMaquina.LIGADA)),
                "Estado do motor difere do estado da maquina.");
        linha.adicionarMaquina(maquina);
        maquina.restaurarEstado(status, inteiro(equipamento, "falhas"), numero(equipamento, "limiar"),
                numero(motor, "saude"), numero(sensor, "calibracao"), integridades);
    }

    private static Produto lerProduto(String tipo, int id, int lote, double risco) {
        switch (tipo) {
            case LpStandard.TIPO: return new LpStandard(id, lote, risco);
            case LpAudiofiloDeluxe.TIPO: return new LpAudiofiloDeluxe(id, lote, risco);
            case CompactoSete.TIPO: return new CompactoSete(id, lote, risco);
            default: throw new IllegalArgumentException("Tipo de disco desconhecido.");
        }
    }

    private static EstrategiaProducao lerEstrategia(String nome) {
        switch (nome) {
            case "EstrategiaFilaGravadoras": return new EstrategiaFilaGravadoras();
            case "EstrategiaGrandeTiragem": return new EstrategiaGrandeTiragem();
            case "EstrategiaMaisDiscos": return new EstrategiaMaisDiscos();
            default: throw new IllegalArgumentException("Estrategia desconhecida.");
        }
    }

    private static Element filho(Element pai, String nome) {
        Element novo = pai.getOwnerDocument().createElement(nome);
        pai.appendChild(novo);
        return novo;
    }

    private static void atributos(Element elemento, Object... pares) {
        for (int i = 0; i < pares.length; i += 2) {
            elemento.setAttribute(pares[i].toString(), String.valueOf(pares[i + 1]));
        }
    }

    private static String texto(Element elemento, String atributo) {
        String valor = elemento.getAttribute(atributo);
        exigir(!valor.isBlank(), "Atributo obrigatorio: " + elemento.getTagName() + "/" + atributo + ".");
        return valor;
    }

    private static int inteiro(Element elemento, String atributo) {
        return Integer.parseInt(texto(elemento, atributo));
    }

    private static double numero(Element elemento, String atributo) {
        double valor = Double.parseDouble(texto(elemento, atributo));
        exigir(Double.isFinite(valor), "Numero nao finito: " + atributo + ".");
        return valor;
    }

    private static List<Element> filhos(Element pai, String nome) {
        List<Element> resultado = new ArrayList<>();
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element && ((Element) no).getTagName().equals(nome)) {
                resultado.add((Element) no);
            }
        }
        return resultado;
    }

    private static Element unico(Element pai, String nome) {
        List<Element> encontrados = filhos(pai, nome);
        exigir(encontrados.size() == 1, "Esperado exatamente um elemento " + nome + ".");
        return encontrados.get(0);
    }

    private static void estrutura(Element elemento, String atributos, String elementos) {
        Set<String> permitidos = Set.of(atributos.split(" "));
        for (String atributo : permitidos) {
            if (!atributo.isEmpty()) { texto(elemento, atributo); }
        }
        for (int i = 0; i < elemento.getAttributes().getLength(); i++) {
            exigir(permitidos.contains(elemento.getAttributes().item(i).getNodeName()),
                    "Atributo desconhecido em " + elemento.getTagName() + ".");
        }
        Set<String> filhosPermitidos = Set.of(elementos.split(" "));
        for (Node no = elemento.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element) {
                exigir(filhosPermitidos.contains(((Element) no).getTagName()),
                        "Elemento desconhecido em " + elemento.getTagName() + ".");
            } else if (no.getNodeType() == Node.TEXT_NODE || no.getNodeType() == Node.CDATA_SECTION_NODE) {
                exigir(no.getTextContent().isBlank(), "Texto inesperado em " + elemento.getTagName() + ".");
            }
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) { throw new IllegalArgumentException(mensagem); }
    }

    private static String explicar(Exception erro) {
        if (erro instanceof NoSuchFileException) {
            return "Arquivo ou diretorio inexistente: " + erro.getMessage();
        }
        if (erro instanceof AccessDeniedException) {
            return "Acesso negado ao arquivo ou caminho de diretorio: " + erro.getMessage();
        }
        if (erro instanceof SAXException) {
            return "XML vazio, malformado ou com estrutura XML nao permitida. " + erro.getMessage();
        }
        return erro.getMessage();
    }
}
