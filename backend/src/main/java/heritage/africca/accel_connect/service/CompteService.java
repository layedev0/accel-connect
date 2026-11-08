package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.Compte;
import heritage.africca.accel_connect.repository.CompteRepository;

import java.util.List;
import java.util.Optional;

public interface CompteService {

    List<Compte> getAllComptes();
    Optional<Compte> getCompteById(Long id);
    Compte createCompte(Compte compte, Long customerId);
    Compte updateCompte(Long id, Compte compteDetails);
    void deleteCompte(Long id);

    Compte depot(Long id, Double montant);

//    Optional<Compte> getCompteByEmail(String email);

}
