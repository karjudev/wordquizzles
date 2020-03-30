package it.unipi.studenti.wordquizzles.client.clients;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import it.unipi.studenti.wordquizzles.shared.Configuration;
import it.unipi.studenti.wordquizzles.shared.rmi.RegistrationService;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * RMIClient è la classe che facilita l'interazione con l'oggetto remoto RMI esposto dal server.
 */
public class RMIClient {

    // Oggetto remoto RMI
    private RegistrationService rmiClient;

    /**
     * Istanzia un nuovo client.
     * 
     * @param hostName Nome dell'host a cui collegarsi.
     * @throws RemoteException Se non è possibile invocare il registry.
     * @throws NotBoundException Se non è stato trovato l'oggetto remoto sul registry.
     */
    public RMIClient(String hostName) throws RemoteException, NotBoundException {
        // Registry che indirizza il client verso l'indirizzo del client RMI
        Registry registry = LocateRegistry.getRegistry(hostName, Configuration.RMI_PORT);
        rmiClient = (RegistrationService) registry.lookup("RegistrationService");
    }

    /**
     * Registra un utente nel sistema.
     * 
     * @param username Username dell'utente da registrare.
     * @param password Password associata allo username.
     * @throws RemoteException Se non è possibile comunicare con l'oggetto remoto.
     * @throws WQPException Se la registrazione non è andata a buon fine.
     */
    public void registerUser(String username, String password) throws RemoteException, WQPException {
        rmiClient.registerUser(username, password);
    }
}