package aeroporto;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Mural de mensagens da simulação (thread-safe).
 * Guarda apenas as últimas mensagens para o painel exibir.
 */
public class Registro {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Deque<String> mensagens = new ArrayDeque<>();
    private final int capacidade;

    public Registro(int capacidade) {
        this.capacidade = capacidade;
    }

    public synchronized void registrar(String texto) {
        mensagens.addLast("[" + LocalTime.now().format(FORMATO) + "] " + texto);
        while (mensagens.size() > capacidade) {
            mensagens.removeFirst();
        }
    }

    public synchronized List<String> ultimas() {
        return new ArrayList<>(mensagens);
    }
}
