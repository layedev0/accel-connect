package heritage.africca.accel_connect.outils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EmpAttachmentId implements Serializable {

    private Long userId;
    private Integer attachId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmpAttachmentId)) return false;
        EmpAttachmentId that = (EmpAttachmentId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(attachId, that.attachId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, attachId); }
}
