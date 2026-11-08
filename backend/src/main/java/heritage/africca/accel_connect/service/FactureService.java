package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.Facture;

import java.util.List;
import java.util.Optional;

public interface FactureService {

    List<Facture> getAllFactures();
    Optional<Facture> getFactureById(Long id);
    Facture saveFacture(Facture facture);
    void deleteFacture(Long id);
    Facture updateFacture(Long id, Facture facture);
}
