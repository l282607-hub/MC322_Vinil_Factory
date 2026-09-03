import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        MateriaPrima pvc = new MateriaPrima(
                1,
                "PVC Reciclado",
                100.0,
                "kg",
                3.0);

        Produto compacto = new Produto(
                1,
                "Compacto 7 Polegadas",
                "aguardando",
                3.0);

        Produto lpStandard = new Produto(
                2,
                "LP Standard 12 Polegadas",
                "aguardando",
                8.0);

        Produto lpDeluxe = new Produto(
                3,
                "LP Duplo Audiofilo Deluxe",
                "aguardando",
                15.0);

        Maquina prensa = new Maquina(
                "Hydraulic Vinyl Press",
                false,
                20.0);

        Esteira esteira = new Esteira(false, 20.0);
        EstacaoInspecao inspecao = new EstacaoInspecao(false);

        exibirIntroducao(pvc, compacto, lpStandard, lpDeluxe);

        int opcao;
        do {
            exibirMenu();
            opcao = lerInteiro(scanner);

            if (opcao == 1) {
                Produto produtoEscolhido = escolherProduto(
                        scanner,
                        compacto,
                        lpStandard,
                        lpDeluxe);

                if (produtoEscolhido != null) {
                    System.out.print("Informe a demanda de PVC em kg: ");
                    double demanda = lerDouble(scanner);
                    produtoEscolhido.definirDemandaMateriaPrima(demanda);

                    produzir(
                            produtoEscolhido,
                            pvc,
                            prensa,
                            esteira,
                            inspecao,
                            demanda);
                }
            } else if (opcao == 2) {
                exibirEstoque(pvc);
            } else if (opcao == 3) {
                System.out.print("Quantidade de PVC que sera adicionada em kg: ");
                double quantidade = lerDouble(scanner);
                pvc.adicionarEstoque(quantidade);
                System.out.println("[OK] Estoque atualizado.");
                exibirEstoque(pvc);
            } else if (opcao == 4) {
                System.out.println("Encerrando a GroovePress Vinyl Works.");
            } else {
                System.out.println("Opcao invalida.");
            }
        } while (opcao != 4);

        scanner.close();
    }

    public static void exibirIntroducao(
            MateriaPrima materiaPrima,
            Produto compacto,
            Produto lpStandard,
            Produto lpDeluxe) {


        System.out.println("========================================");
        System.out.println("GROOVEPRESS VINYL WORKS");
        System.out.println("Fabrica artesanal de discos de vinil");
        System.out.println("========================================");

        System.out.println("Materia-prima principal: " + materiaPrima.getNome());
        System.out.println("Estoque inicial: " + materiaPrima.getQuantidade()
                + " " + materiaPrima.getUnidade());


        System.out.println("Produtos disponiveis:");
        System.out.println("1 - " + compacto.getNome()
                + " (demanda inicial: " + compacto.getDemandaMateriaPrima() + " kg)");
        System.out.println("2 - " + lpStandard.getNome()
                + " (demanda inicial: " + lpStandard.getDemandaMateriaPrima() + " kg)");
        System.out.println("3 - " + lpDeluxe.getNome()
                + " (demanda inicial: " + lpDeluxe.getDemandaMateriaPrima() + " kg)");


        System.out.println("Desenvolvido por:\nJeorde Antonio - RA 295164\nLeo Bertoli - RA 282607");
    }

    public static void exibirMenu() {

        System.out.println("\n========================================");
        System.out.println("MENU PRINCIPAL");
        System.out.println("========================================");

        System.out.println("1 - Iniciar producao");
        System.out.println("2 - Consultar estoque");
        System.out.println("3 - Adicionar PVC ao estoque");
        System.out.println("4 - Sair");
        System.out.print("Escolha: ");
    }

    public static Produto escolherProduto(
            Scanner scanner,
            Produto compacto,
            Produto lpStandard,
            Produto lpDeluxe) {

        System.out.println("\nSelecione o produto:");
        System.out.println("1 - " + compacto.getNome());
        System.out.println("2 - " + lpStandard.getNome());
        System.out.println("3 - " + lpDeluxe.getNome());
        System.out.print("Escolha: ");

        int escolha = lerInteiro(scanner);

        if (escolha == 1) {
            return compacto;
        } else if (escolha == 2) {
            return lpStandard;
        } else if (escolha == 3) {
            return lpDeluxe;
        }

        System.out.println("Produto invalido.");
        return null;
    }

    public static void produzir(
            Produto produto,
            MateriaPrima pvc,
            Maquina prensa,
            Esteira esteira,
            EstacaoInspecao inspecao,
            double demanda) {

        System.out.println("\n[OK] Verificando disponibilidade de " + pvc.getNome() + "...");

        if (!pvc.verificarDisponibilidade(demanda)) {
            System.out.println("[ERRO] Demanda abaixo do minimo ou estoque insuficiente.");
            return;
        }

        if (!prensa.verificarCapacidade(demanda)) {
            System.out.println("[ERRO] A demanda excede a capacidade da maquina.");
            return;
        }

        if (!esteira.verificarCapacidade(demanda)) {
            System.out.println("[ERRO] A demanda excede a capacidade da esteira.");
            return;
        }

        esteira.ligar();
        prensa.ligar();
        System.out.println("[OK] Esteira ligada.");
        System.out.println("[OK] Maquina " + prensa.getNome() + " ligada.");

        if (!esteira.adicionarItem(pvc, demanda)) {
            System.out.println("[ERRO] Nao foi possivel colocar a materia-prima na esteira.");
            desligarEquipamentos(prensa, esteira, inspecao);
            return;
        }

        System.out.println("[OK] Materia-prima colocada na esteira.");
        esteira.removerItem();
        System.out.println("[OK] Materia-prima transportada ate a maquina.");

        if (!prensa.processar(pvc, demanda)) {
            System.out.println("[ERRO] A maquina nao conseguiu processar o produto.");
            desligarEquipamentos(prensa, esteira, inspecao);
            return;
        }

        produto.processar();
        produto.definirMateriaPrimaUtilizada(pvc);
        System.out.println("[OK] Produto " + produto.getNome() + " processado.");

        if (!esteira.adicionarItem(produto, demanda)) {
            System.out.println("[ERRO] Nao foi possivel colocar o produto na esteira.");
            desligarEquipamentos(prensa, esteira, inspecao);
            return;
        }

        esteira.removerItem();
        System.out.println("[OK] Produto transportado para a inspecao.");

        inspecao.ativar();
        System.out.println("[OK] Estacao de inspecao ativada.");

        if (!inspecao.inspecionar(produto)) {
            System.out.println("[ERRO] O produto nao foi inspecionado.");
            desligarEquipamentos(prensa, esteira, inspecao);
            return;
        }

        System.out.println("[OK] Produto aprovado na inspecao.");
        System.out.println("========================================");
        System.out.println("PRODUCAO CONCLUIDA COM SUCESSO");
        System.out.println("========================================");
        System.out.println("Status do produto: " + produto.getStatus());
        System.out.println("Materia-prima utilizada: "
                + produto.getMateriaPrimaUtilizada().getId() + " - "
                + produto.getMateriaPrimaUtilizada().getNome());
        exibirEstoque(pvc);

        desligarEquipamentos(prensa, esteira, inspecao);
    }

    public static void exibirEstoque(MateriaPrima materiaPrima) {
        System.out.println("Estoque de " + materiaPrima.getNome() + ": "
                + materiaPrima.getQuantidade() + " " + materiaPrima.getUnidade());
    }

    public static void desligarEquipamentos(
            Maquina maquina,
            Esteira esteira,
            EstacaoInspecao inspecao) {

        maquina.desligar();
        esteira.desligar();
        inspecao.desativar();
    }

    public static int lerInteiro(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.print("Entrada invalida. Digite um numero inteiro: ");
            scanner.next();
        }

        return scanner.nextInt();
    }

    public static double lerDouble(Scanner scanner) {
        while (true) {
            if (scanner.hasNextDouble()) {
                double valor = scanner.nextDouble();
                if (valor > 0) {
                    return valor;
                }
            } else {
                scanner.next();
            }

            System.out.print("Entrada invalida. Digite um numero maior que zero: ");
        }
    }
}
