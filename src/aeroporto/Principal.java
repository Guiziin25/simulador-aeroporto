package aeroporto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Ponto de entrada. Uso: java -ea -cp out aeroporto.Principal config/C1.properties
 */
public class Principal {

    public static void main(String[] args) throws Exception {
        String caminho = args.length > 0 ? args[0] : "config/C1.properties";
        Configuracao configuracao;
        try {
            configuracao = Configuracao.carregar(caminho);
        } catch (IOException e) {
            System.err.println("Não foi possível ler a configuração '" + caminho + "': " + e.getMessage());
            return;
        }

        Registro registro = new Registro(12);

        List<Pista> pistas = new ArrayList<>();
        for (int i = 1; i <= configuracao.pistas; i++) {
            pistas.add(new Pista(i));
        }

        Torre torre = new Torre(pistas, registro);
        Thread threadTorre = new Thread(torre, "torre");
        threadTorre.setDaemon(true);
        threadTorre.start();

        Painel painel = new Painel(torre, registro, configuracao.intervaloPainelMs, configuracao.limparTela);
        painel.iniciar();

        int total = configuracao.avioesParaPousar + configuracao.avioesParaDecolar;
        List<String> codigos = gerarCodigos(total);
        List<Aviao> avioes = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            boolean comecaNoAr = i < configuracao.avioesParaPousar;
            avioes.add(new Aviao(codigos.get(i), comecaNoAr, torre, registro, configuracao));
        }
        for (Aviao aviao : avioes) {
            aviao.start();
        }
        for (Aviao aviao : avioes) {
            aviao.join();
        }
        painel.parar();
    }

    /** Códigos de voo de empresas brasileiras: AD = Azul, G3 = Gol, LA = LATAM. */
    private static List<String> gerarCodigos(int quantidade) {
        String[] companhias = {"AD", "G3", "LA"};
        Random sorteio = new Random(2774);
        Set<String> usados = new LinkedHashSet<>();
        while (usados.size() < quantidade) {
            usados.add(companhias[usados.size() % companhias.length] + " " + (1000 + sorteio.nextInt(9000)));
        }
        return new ArrayList<>(usados);
    }
}
