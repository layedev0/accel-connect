package heritage.africca.accel_connect.entity;

import heritage.africca.accel_connect.outils.EmpAttachmentId;
import jakarta.persistence.*;
import lombok.*;


@Setter
@Getter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@IdClass(EmpAttachmentId.class)
public class EmployeeAttachment extends DateAudit {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "eattach_id", nullable = false)
    private Integer attachId;

    @Column(name = "eattach_desc", length = 200)
    private String description;

    @Column(name = "eattach_filename", length = 100)
    private String filename;

    @Column(name = "eattach_size")
    private Integer size;

    @Column(name = "eattach_attachment", columnDefinition = "bytea")
    private byte[] attachment;

    @Column(name = "eattach_type", length = 200)
    private String fileType;

    @Column(name = "screen", length = 100)
    private String screen;

    @Column(name = "attached_by")
    private Integer attachedBy;

    @Column(name = "attached_by_name", length = 200)
    private String attachedByName;
}
