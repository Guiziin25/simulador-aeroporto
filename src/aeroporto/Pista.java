package aeroporto;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Pista de pouso/decolagem: recurso compartilhado de acesso exclusivo.
 *
 * A pista NÃO impede a corrida por conta própria: quem garante a exclusão é a Torre.
 * Se a Torre falhar, a pista percebe a violação e lança ConflitoDePistaException.
 */
public class Pista {

    private final int numero;
    private final AtomicReference<Aviao> ocupante = new AtomicReference<>();

    public Pista(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    public Aviao getOcupante() {
        return ocupante.get();
    }

    public boolean estaLivre() {
        return ocupante.get() == null;
    }

    /** Marca a pista como ocupada pelo avião; se já estiver ocupada, é uma race condition. */
    public void reservar(Aviao aviao) {
        if (!ocupante.compareAndSet(null, aviao)) {
            throw new ConflitoDePistaException("CONFLITO na pista " + numero + ": o avião "
                    + aviao + " tentou usar a pista, mas ela já está ocupada pelo avião "
                    + ocupante.get() + ".");
        }
    }

    /** Libera a pista; só o avião que a ocupa pode liberá-la. */
    public void liberar(Aviao aviao) {
        if (!ocupante.compareAndSet(aviao, null)) {
            throw new ConflitoDePistaException("CONFLITO na pista " + numero + ": o avião "
                    + aviao + " tentou liberar uma pista que está ocupada por "
                    + ocupante.get() + ".");
        }
    }
}
