package heritage.africca.accel_connect.controller;


import heritage.africca.accel_connect.entity.Facture;
import heritage.africca.accel_connect.service.FactureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/factures")
public class FactureController {


    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping
    public ResponseEntity<List<Facture>> getAllFactures() {
        List<Facture> factures = factureService.getAllFactures();
        return new ResponseEntity<>(factures, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Facture> getFactureById(@PathVariable Long id) {
        Optional<Facture> facture = factureService.getFactureById(id);
        return facture.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Facture> createFacture(@RequestBody Facture facture) {
        Facture savedFacture = factureService.saveFacture(facture);
        return new ResponseEntity<>(savedFacture, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Facture> updateFacture(@PathVariable Long id, @RequestBody Facture facture) {
        Facture updatedFacture = factureService.updateFacture(id, facture);
        if (updatedFacture != null) {
            return new ResponseEntity<>(updatedFacture, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFacture(@PathVariable Long id) {
        factureService.deleteFacture(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
