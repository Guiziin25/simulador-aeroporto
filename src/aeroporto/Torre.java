package aeroporto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Torre de controle: coordenadora central.
 *
 * Os aviões entram nas filas (pedidos) e esperam. A própria Torre roda numa thread
 * que, a cada passo, pega o próximo pedido (pouso tem prioridade sobre decolagem),
 * procura uma pista livre, reserva a pista e concede a autorização.
 *
 * VERSÃO INICIAL: funciona, mas a Antonio deve revisar e aprimorar
 * (justiça entre as filas, mais asserções, explicação dos mecanismos no README).
 */
public class Torre implements Runnable {

    private enum Tipo { POUSO, DECOLAGEM }

    private static class Pedido {
        final Aviao aviao;
        final Tipo tipo;
        final CompletableFuture<Pista> autorizacao = new CompletableFuture<>();

        Pedido(Aviao aviao, Tipo tipo) {
            this.aviao = aviao;
            this.tipo = tipo;
        }
    }

    private final List<Pista> pistas;
    private final Registro registro;
    private final Deque<Pedido> filaPouso = new ArrayDeque<>();
    private final Deque<Pedido> filaDecolagem = new ArrayDeque<>();

    private final ReentrantLock trinco = new ReentrantLock();
    private final Condition haTrabalho = trinco.newCondition();

    public Torre(List<Pista> pistas, Registro registro) {
        this.pistas = pistas;
        this.registro = registro;
    }

    // ------------------------------------------------------------------
    // API usada pelos aviões
    // ------------------------------------------------------------------

    public Pista solicitarPouso(Aviao aviao) throws InterruptedException {
        return solicitar(aviao, Tipo.POUSO);
    }

    public Pista solicitarDecolagem(Aviao aviao) throws InterruptedException {
        return solicitar(aviao, Tipo.DECOLAGEM);
    }

    public void liberarPista(Pista pista, Aviao aviao) {
        trinco.lock();
        try {
            pista.liberar(aviao);
            registro.registrar("TORRE: pista " + pista.getNumero() + " liberada por " + aviao);
            haTrabalho.signalAll();
        } finally {
            trinco.unlock();
        }
    }

    public EstadoTorre obterEstado() {
        trinco.lock();
        try {
            List<String> pouso = new ArrayList<>();
            for (Pedido p : filaPouso) {
                pouso.add(p.aviao.getCodigo());
            }
            List<String> decolagem = new ArrayList<>();
            for (Pedido p : filaDecolagem) {
                decolagem.add(p.aviao.getCodigo());
            }
            List<String> situacaoPistas = new ArrayList<>();
            for (Pista pista : pistas) {
                Aviao ocupante = pista.getOcupante();
                situacaoPistas.add("Pista " + pista.getNumero() + ": "
                        + (ocupante == null ? "LIVRE" : "OCUPADA por " + ocupante.getCodigo()));
            }
            return new EstadoTorre(pouso, decolagem, situacaoPistas);
        } finally {
            trinco.unlock();
        }
    }

    // ------------------------------------------------------------------
    // Funcionamento interno
    // ------------------------------------------------------------------

    private Pista solicitar(Aviao aviao, Tipo tipo) throws InterruptedException {
        Pedido pedido = new Pedido(aviao, tipo);
        trinco.lock();
        try {
            fila(tipo).addLast(pedido);
            registro.registrar("TORRE: recebeu pedido de " + descricao(tipo) + " de " + aviao);
            haTrabalho.signalAll();
        } finally {
            trinco.unlock();
        }
        try {
            return pedido.autorizacao.get(); // o avião espera aqui pela autorização
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa; // ex.: ConflitoDePistaException
            }
            throw new IllegalStateException(e.getCause());
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                autorizarProximo();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void autorizarProximo() throws InterruptedException {
        trinco.lock();
        try {
            Pedido pedido;
            Pista pista;
            while (true) {
                pedido = proximoPedido();
                pista = (pedido == null) ? null : buscarPistaLivre();
                if (pedido != null && pista != null) {
                    break;
                }
                haTrabalho.await(); // sem pedido ou sem pista livre: espera um sinal
            }
            fila(pedido.tipo).removeFirst();
            try {
                pista.reservar(pedido.aviao);
                assert pista.getOcupante() == pedido.aviao : "Pista reservada deve pertencer ao avião autorizado";
                registro.registrar("TORRE: autoriza " + pedido.aviao + " a usar a pista "
                        + pista.getNumero() + " (" + descricao(pedido.tipo) + ")");
                pedido.autorizacao.complete(pista);
            } catch (ConflitoDePistaException e) {
                registro.registrar("!!! " + e.getMessage());
                pedido.autorizacao.completeExceptionally(e);
            }
        } finally {
            trinco.unlock();
        }
    }

    /** Pouso tem prioridade sobre decolagem. TODO (Antonio): evitar starvation da decolagem. */
    private Pedido proximoPedido() {
        if (!filaPouso.isEmpty()) {
            return filaPouso.peekFirst();
        }
        return filaDecolagem.peekFirst();
    }

    private Pista buscarPistaLivre() {
        for (Pista pista : pistas) {
            // >>> CONTROLE DE CONCORRÊNCIA: é esta condição que impede dois aviões na mesma pista.
            // >>> Para provocar a race condition, troque "pista.estaLivre()" por "true".
            if (pista.estaLivre()) {
                return pista;
            }
        }
        return null;
    }

    private Deque<Pedido> fila(Tipo tipo) {
        return tipo == Tipo.POUSO ? filaPouso : filaDecolagem;
    }

    private String descricao(Tipo tipo) {
        return tipo == Tipo.POUSO ? "pouso" : "decolagem";
    }
}
