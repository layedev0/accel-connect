package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.entity.Compte;
import heritage.africca.accel_connect.service.CompteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping ("/api/comptes")
@CrossOrigin("*")
public class CompteController {


    private final CompteService compteService;

    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @GetMapping
    public ResponseEntity<List<Compte>> getAllComptes() {
        List<Compte> comptes = compteService.getAllComptes();
        return new ResponseEntity<>(comptes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compte> getCompteById(@PathVariable Long id) {
        Optional<Compte> compte = compteService.getCompteById(id);
        return compte.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Compte> createCompte(@RequestBody Compte compte, @RequestParam Long customerId) {
        Compte savedCompte = compteService.createCompte(compte, customerId);
        return new ResponseEntity<>(savedCompte, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compte> updateCompte(@PathVariable Long id, @RequestBody Compte compte) {
        Compte updatedCompte = compteService.updateCompte(id, compte);
        if (updatedCompte != null) {
            return new ResponseEntity<>(updatedCompte, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompte(@PathVariable Long id) {
        compteService.deleteCompte(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
