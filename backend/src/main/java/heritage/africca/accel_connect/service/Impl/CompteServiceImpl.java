package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.entity.Compte;
import heritage.africca.accel_connect.entity.Customer;
import heritage.africca.accel_connect.entity.enumeration.TransactionType;
import heritage.africca.accel_connect.repository.CompteRepository;
import heritage.africca.accel_connect.service.CompteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CompteServiceImpl implements CompteService {



    private final CompteRepository compteRepository;

    public CompteServiceImpl(CompteRepository compteRepository) {
        this.compteRepository = compteRepository;
    }

    public List<Compte> getAllComptes() {
        return compteRepository.findAll();
    }

    public Optional<Compte> getCompteById(Long id) {
        return compteRepository.findById(id);
    }

    public Compte createCompte(Compte compte, Long customerId) {
        Customer customer = compteRepository.findCustomerById(customerId);
        compte.setCustomer(customer);
        return compteRepository.save(compte);
    }

    public Compte updateCompte(Long id, Compte compteDetails) {
        return compteRepository.findById(id).map(compte -> {
            compte.setCustomer(compteDetails.getCustomer());
            return compteRepository.save(compte);
        }).orElseThrow(() -> new RuntimeException("Compte not found with id " + id));
    }

    public void deleteCompte(Long id) {
        compteRepository.deleteById(id);
    }

    @Override
    public Compte depot(Long id, Double montant) {

        compteRepository.findById(id).map(compte -> {
            compte.setSolde(compte.getSolde() + montant);
            compte.setTransactType(String.valueOf(TransactionType.CREDIT));
            return compteRepository.save(compte);
        }).orElseThrow(() -> new RuntimeException("Compte not found with id " + id));

        return  null;
    }

//    @Override
//    public Compte getCompteByEmail(String email) {
//
//        Compte compte = new Compte();
//        compte = compteRepository.findCompteByEmail(email).orElseThrow(() -> new RuntimeException("Compte not found with email " + email));
//
//        return compte;
//
//    }
    }
