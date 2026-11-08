package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCommentRequest {

    @NotBlank(message = "Le commentaire ne peut pas être vide")
    @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
    private String content;
}
