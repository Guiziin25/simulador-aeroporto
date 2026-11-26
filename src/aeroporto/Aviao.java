package aeroporto;

/**
 * Avião: entidade concorrente (uma thread) com ciclo de vida infinito.
 *
 * Quem começa no ar: pede pouso, pousa, espera, pede decolagem, decola, volta a pedir pouso...
 * Quem começa no solo: pede decolagem, decola, volta a pedir pouso, pousa...
 * O avião só se comunica com a Torre, nunca com outro avião.
 */
public class Aviao extends Thread {

    private final String codigo;
    private final Torre torre;
    private final Registro registro;
    private final Configuracao configuracao;
    private boolean noAr;

    public Aviao(String codigo, boolean comecaNoAr, Torre torre, Registro registro, Configuracao configuracao) {
        super("aviao-" + codigo);
        this.codigo = codigo;
        this.noAr = comecaNoAr;
        this.torre = torre;
        this.registro = registro;
        this.configuracao = configuracao;
    }

    public String getCodigo() {
        return codigo;
    }

    @Override
    public String toString() {
        return codigo;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                if (noAr) {
                    pousar();
                } else {
                    decolar();
                }
            }
        } catch (InterruptedException e) {
            interrupt();
        } catch (ConflitoDePistaException e) {
            registro.registrar("!!! " + codigo + " encerrado por erro de concorrência: " + e.getMessage());
            throw e;
        }
    }

    private void pousar() throws InterruptedException {
        registro.registrar(codigo + " pede autorização para POUSAR");
        Pista pista = torre.solicitarPouso(this);
        assert pista.getOcupante() == this : "O avião só pode usar a pista depois de autorizado pela Torre";
        registro.registrar(codigo + " POUSANDO na pista " + pista.getNumero());
        Thread.sleep(configuracao.tempoOperacaoMs);
        torre.liberarPista(pista, this);
        registro.registrar(codigo + " pousou e está no solo");
        noAr = false;
        Thread.sleep(configuracao.delayAposPousoMs);
    }

    private void decolar() throws InterruptedException {
        registro.registrar(codigo + " pede autorização para DECOLAR");
        Pista pista = torre.solicitarDecolagem(this);
        assert pista.getOcupante() == this : "O avião só pode usar a pista depois de autorizado pela Torre";
        registro.registrar(codigo + " DECOLANDO na pista " + pista.getNumero());
        Thread.sleep(configuracao.tempoOperacaoMs);
        torre.liberarPista(pista, this);
        registro.registrar(codigo + " decolou e está no ar");
        noAr = true;
        Thread.sleep(configuracao.delayAposDecolagemMs);
    }
}
