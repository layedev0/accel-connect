package heritage.africca.accel_connect.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@Data
@RequiredArgsConstructor
public class ProfilDetailsDTO {
    private String  jobProfile;
    private String username;
    private String statut;
    private String dateAdhesion;
    private String anniversaire;
    private String contact;
    private String email;
    private String departement;

}
