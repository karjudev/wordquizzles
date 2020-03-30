package it.unipi.studenti.wordquizzles.shared.wqp;

/**
 * WQPException è l'eccezione scatenata quando il client riceve una Response WQP di tipo error.
 */
public class WQPException extends Exception {

    private static final long serialVersionUID = -8012542202046578282L;

    /**
     * Costruisce l'eccezione con un certo messaggio.
     * 
     * @param message Messaggio associato all'eccezione.
     */
    public WQPException(String message) {
        super(message);
    }
}