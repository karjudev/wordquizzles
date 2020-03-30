package it.unipi.studenti.wordquizzles.shared.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;


/**
 * RegistrationService è l'interfaccia dell'oggetto remoto RMI che espone il metodo necessario a registrare un nuovo utente.
 */
public interface RegistrationService extends Remote {

    /**
     * Registra un utente nel sistema.
     * @param username               Nome dell'utente da registrare.
     * @param password               Password associata all'utente.
     * @throws RemoteException       Se c'è un problema nell'esecuzione del metodo remoto.
     * @throws WQPException          Se l'utente che si vuole registrare esiste già.
     */
    public void registerUser(String username, String password) throws RemoteException, WQPException;
}