import java.util.Scanner;

/**
 * Leitura validada do console. Toda entrada do usuario passa por aqui: o
 * texto e convertido e verificado, e uma entrada invalida gera uma mensagem
 * clara e uma nova pergunta, nunca o encerramento do programa.
 *
 * <p>A leitura e feita linha a linha, o que evita misturar {@code nextInt} e
 * {@code nextLine} e permite tratar o fim da entrada.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see EntradaInvalidaException
 */
public class LeitorConsole {
    /** Valor devolvido pelas leituras quando a entrada acaba (Ctrl+D / Ctrl+Z). */
    public static final int FIM_DA_ENTRADA = -1;

    private final Scanner scanner;

    /**
     * Cria um leitor sobre um {@link Scanner} existente.
     *
     * @param scanner fonte das linhas digitadas
     */
    public LeitorConsole(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Le uma opcao inteira dentro de um intervalo, repetindo a pergunta ate
     * receber um valor valido.
     *
     * @param minimo menor valor aceito
     * @param maximo maior valor aceito
     * @return a opcao digitada, ou {@link #FIM_DA_ENTRADA} se a entrada acabou
     */
    public int lerOpcao(int minimo, int maximo) {
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                return FIM_DA_ENTRADA;
            }
            try {
                return converterOpcao(scanner.nextLine(), minimo, maximo);
            } catch (EntradaInvalidaException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Le uma quantidade decimal maior ou igual a zero. Aceita ponto ou virgula
     * como separador decimal e rejeita texto, NaN e infinitos.
     *
     * @return a quantidade digitada, ou {@link #FIM_DA_ENTRADA} se a entrada
     *         acabou
     */
    public double lerQuantidade() {
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                return FIM_DA_ENTRADA;
            }
            try {
                return converterQuantidade(scanner.nextLine());
            } catch (EntradaInvalidaException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /** Pausa ate o usuario apertar ENTER, para que a mensagem seja lida. */
    public void aguardarEnter() {
        System.out.print("Pressione ENTER para continuar...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
        System.out.println();
    }

    /**
     * Converte um texto em opcao inteira dentro do intervalo.
     *
     * @throws EntradaInvalidaException se o texto estiver vazio, nao for um
     *                                  inteiro ou estiver fora do intervalo
     */
    private int converterOpcao(String texto, int minimo, int maximo) {
        String limpo = texto.trim();
        String esperado = maximo == Integer.MAX_VALUE
                ? "um numero inteiro maior ou igual a " + minimo
                : "um numero inteiro entre " + minimo + " e " + maximo;
        if (limpo.isEmpty()) {
            throw new EntradaInvalidaException("Entrada vazia. Digite " + esperado + ".");
        }
        int valor;
        try {
            valor = Integer.parseInt(limpo);
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("'" + limpo + "' nao e um numero inteiro valido. Digite "
                    + esperado + ".");
        }
        if (valor < minimo || valor > maximo) {
            throw new EntradaInvalidaException("Opcao inexistente: " + valor + ". Digite " + esperado + ".");
        }
        return valor;
    }

    /**
     * Converte um texto em quantidade decimal nao negativa.
     *
     * @throws EntradaInvalidaException se o texto estiver vazio, nao for um
     *                                  numero, nao for finito ou for negativo
     */
    private double converterQuantidade(String texto) {
        String limpo = texto.trim().replace(',', '.');
        if (limpo.isEmpty()) {
            throw new EntradaInvalidaException("Entrada vazia. Digite uma quantidade, ou 0 para voltar.");
        }
        double valor;
        try {
            valor = Double.parseDouble(limpo);
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("'" + limpo + "' nao e um numero. "
                    + "Use ponto ou virgula como separador decimal.");
        }
        if (!Double.isFinite(valor) || valor < 0) {
            throw new EntradaInvalidaException("Digite uma quantidade finita, maior ou igual a zero.");
        }
        return valor;
    }
}
