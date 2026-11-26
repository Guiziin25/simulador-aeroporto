package aeroporto;

/**
 * Lançada quando o controle de concorrência falha e duas operações
 * disputam a mesma pista (race condition não tratada).
 * A mensagem sempre diz qual pista e quais aviões estão envolvidos.
 */
public class ConflitoDePistaException extends RuntimeException {

    public ConflitoDePistaException(String mensagem) {
        super(mensagem);
    }
}
