package it.unipi.studenti.wordquizzles.server.rmi;

import java.rmi.RemoteException;

import it.unipi.studenti.wordquizzles.server.services.AccountService;
import it.unipi.studenti.wordquizzles.shared.rmi.RegistrationService;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * RegistrationServiceImplementation implementa l'interfaccia definita
 * permettendo di registrare l'utente nel sistema.
 */
public class RegistrationServiceImplementation implements RegistrationService {

    // Riferimento al servizio di registrazione degli account
    private AccountService accountService;

    /**
     * Crea
     */
    public RegistrationServiceImplementation() {
        accountService = AccountService.getInstance();
    }

    @Override
    public void registerUser(String username, String password) throws RemoteException, WQPException {
        System.out.printf("[REGISTRATION] Received register request from %s\n", username);
        accountService.register(username, password);
        System.out.printf("[REGISTRATION] Successifully registered user %s\n", username);
    }

    
}