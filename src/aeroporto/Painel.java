package aeroporto;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Painel de controle: desenha no terminal, a cada intervalo configurado,
 * as filas, o estado das pistas e as últimas mensagens da simulação.
 */
public class Painel {

    private final Torre torre;
    private final Registro registro;
    private final long intervaloMs;
    private final boolean limparTela;
    private ScheduledExecutorService agendador;

    public Painel(Torre torre, Registro registro, long intervaloMs, boolean limparTela) {
        this.torre = torre;
        this.registro = registro;
        this.intervaloMs = intervaloMs;
        this.limparTela = limparTela;
    }

    public void iniciar() {
        agendador = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "painel");
            thread.setDaemon(true);
            return thread;
        });
        agendador.scheduleAtFixedRate(this::desenhar, 0, intervaloMs, TimeUnit.MILLISECONDS);
    }

    public void parar() {
        if (agendador != null) {
            agendador.shutdownNow();
        }
    }

    private void desenhar() {
        try {
            EstadoTorre estado = torre.obterEstado();
            StringBuilder tela = new StringBuilder();
            if (limparTela) {
                tela.append("\033[H\033[2J");
            }
            tela.append("=========== AEROPORTO - PAINEL DE CONTROLE ===========\n\n");
            tela.append("PISTAS\n");
            for (String linha : estado.pistas()) {
                tela.append("  ").append(linha).append('\n');
            }
            tela.append("\nFILA DE POUSO     : ").append(formatar(estado.filaPouso())).append('\n');
            tela.append("FILA DE DECOLAGEM : ").append(formatar(estado.filaDecolagem())).append("\n\n");
            tela.append("ÚLTIMAS MENSAGENS\n");
            for (String mensagem : registro.ultimas()) {
                tela.append("  ").append(mensagem).append('\n');
            }
            System.out.print(tela);
            System.out.flush();
        } catch (RuntimeException e) {
            // Uma falha ao desenhar não deve cancelar o agendamento.
            System.err.println("Falha no painel: " + e);
        }
    }

    private String formatar(List<String> fila) {
        return fila.isEmpty() ? "(vazia)" : String.join(" -> ", fila);
    }
}
