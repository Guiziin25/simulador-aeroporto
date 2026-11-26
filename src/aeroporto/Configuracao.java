package aeroporto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Configuração inicial da simulação, lida de um arquivo .properties.
 */
public class Configuracao {

    public final int avioesParaPousar;
    public final int avioesParaDecolar;
    public final int pistas;
    public final long tempoOperacaoMs;
    public final long delayAposPousoMs;
    public final long delayAposDecolagemMs;
    public final long intervaloPainelMs;
    public final boolean limparTela;

    private Configuracao(Properties p) {
        this.avioesParaPousar = inteiro(p, "avioesParaPousar", 3);
        this.avioesParaDecolar = inteiro(p, "avioesParaDecolar", 3);
        this.pistas = inteiro(p, "pistas", 2);
        this.tempoOperacaoMs = inteiro(p, "tempoOperacaoMs", 2000);
        this.delayAposPousoMs = inteiro(p, "delayAposPousoMs", 3000);
        this.delayAposDecolagemMs = inteiro(p, "delayAposDecolagemMs", 5000);
        this.intervaloPainelMs = inteiro(p, "intervaloPainelMs", 1000);
        this.limparTela = Boolean.parseBoolean(p.getProperty("limparTela", "true"));

        if (pistas < 1) {
            throw new IllegalArgumentException("É preciso ao menos 1 pista.");
        }
        if (avioesParaPousar + avioesParaDecolar < 1) {
            throw new IllegalArgumentException("É preciso ao menos 1 avião.");
        }
    }

    public static Configuracao carregar(String caminho) throws IOException {
        Properties propriedades = new Properties();
        try (FileInputStream entrada = new FileInputStream(caminho);
             InputStreamReader leitor = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
            propriedades.load(leitor);
        }
        return new Configuracao(propriedades);
    }

    private static int inteiro(Properties p, String chave, int padrao) {
        String valor = p.getProperty(chave);
        return (valor == null || valor.isBlank()) ? padrao : Integer.parseInt(valor.trim());
    }
}
