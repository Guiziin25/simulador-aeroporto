package aeroporto;

import java.util.List;

/** Cópia (snapshot) do estado da Torre, usada pelo painel sem expor estruturas internas. */
public record EstadoTorre(List<String> filaPouso, List<String> filaDecolagem, List<String> pistas) {
}
