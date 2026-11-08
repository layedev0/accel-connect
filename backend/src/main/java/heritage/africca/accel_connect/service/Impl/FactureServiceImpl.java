package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.entity.Facture;
import heritage.africca.accel_connect.repository.FactureRepository;
import heritage.africca.accel_connect.service.FactureService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FactureServiceImpl implements FactureService {



    private final FactureRepository factureRepository;

    public FactureServiceImpl(FactureRepository factureRepository) {
        this.factureRepository = factureRepository;
    }

    public List<Facture> getAllFactures() {
        return factureRepository.findAll();
    }

    public Optional<Facture> getFactureById(Long id) {
        return factureRepository.findById(id);
    }

    public Facture saveFacture(Facture facture) {
        return factureRepository.save(facture);
    }

    public Facture updateFacture(Long id, Facture facture) {
        Optional<Facture> existingFacture = factureRepository.findById(id);
        if (existingFacture.isPresent()) {
            Facture factureToUpdate = existingFacture.get();
            factureToUpdate.setDate(facture.getDate());
            return factureRepository.save(factureToUpdate);
        }
        return null;
    }

    public void deleteFacture(Long id) {
        factureRepository.deleteById(id);
    }
}
