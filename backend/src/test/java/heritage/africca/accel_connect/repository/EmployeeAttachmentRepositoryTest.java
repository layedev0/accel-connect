package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.outils.EmpAttachmentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EmployeeAttachmentRepository")
class EmployeeAttachmentRepositoryTest {

    @Autowired
    private EmployeeAttachmentRepository repository;

    private static final Long USER_ID = 1L;

    private EmployeeAttachment save(Long userId, Integer attachId, String screen) {
        EmployeeAttachment a = new EmployeeAttachment();
        a.setUserId(userId);
        a.setAttachId(attachId);
        a.setFilename("file_" + attachId + ".pdf");
        a.setSize(1024);
        a.setFileType("application/pdf");
        a.setDescription("Test doc");
        a.setScreen(screen);
        a.setAttachedBy(99);
        a.setAttachedByName("Test User");
        a.setAttachment(new byte[]{1, 2, 3});
        return repository.save(a);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc")
    class FindByUserId {

        @Test
        @DisplayName("returns all attachments for user ordered by createdAt desc")
        void returnsAllForUser() {
            save(USER_ID, 1, "contrat");
            save(USER_ID, 2, "bulletin-salaire");
            save(USER_ID, 3, "attestation-travail");

            List<EmployeeAttachment> result = repository.findByUserIdOrderByCreatedAtDesc(USER_ID);

            assertEquals(3, result.size());
            result.forEach(a -> assertEquals(USER_ID, a.getUserId()));
        }

        @Test
        @DisplayName("returns empty list when user has no attachments")
        void returnsEmptyForUnknownUser() {
            List<EmployeeAttachment> result = repository.findByUserIdOrderByCreatedAtDesc(999L);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("does not return attachments of other users")
        void doesNotReturnOtherUsers() {
            save(USER_ID, 1, "contrat");
            save(2L, 1, "contrat");

            List<EmployeeAttachment> result = repository.findByUserIdOrderByCreatedAtDesc(USER_ID);

            assertEquals(1, result.size());
            assertEquals(USER_ID, result.get(0).getUserId());
        }
    }

    @Nested
    @DisplayName("findByUserIdAndAttachId")
    class FindByUserIdAndAttachId {

        @Test
        @DisplayName("returns attachment when found")
        void returnsWhenFound() {
            save(USER_ID, 5, "bulletin-salaire");

            Optional<EmployeeAttachment> result = repository.findByUserIdAndAttachId(USER_ID, 5);

            assertTrue(result.isPresent());
            assertEquals(5, result.get().getAttachId());
            assertEquals("file_5.pdf", result.get().getFilename());
        }

        @Test
        @DisplayName("returns empty when attachId does not exist")
        void returnsEmptyWhenNotFound() {
            Optional<EmployeeAttachment> result = repository.findByUserIdAndAttachId(USER_ID, 999);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty when userId does not match")
        void returnsEmptyWhenUserIdMismatch() {
            save(USER_ID, 1, "contrat");
            Optional<EmployeeAttachment> result = repository.findByUserIdAndAttachId(2L, 1);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findMaxAttachIdByUserId")
    class FindMaxAttachId {

        @Test
        @DisplayName("returns max attachId for user")
        void returnsMaxId() {
            save(USER_ID, 1, "contrat");
            save(USER_ID, 3, "bulletin-salaire");
            save(USER_ID, 2, "attestation-travail");

            Optional<Integer> max = repository.findMaxAttachIdByUserId(USER_ID);

            assertTrue(max.isPresent());
            assertEquals(3, max.get());
        }

        @Test
        @DisplayName("returns empty when user has no attachments")
        void returnsEmptyWhenNone() {
            Optional<Integer> max = repository.findMaxAttachIdByUserId(USER_ID);
            assertTrue(max.isEmpty());
        }

        @Test
        @DisplayName("is scoped per user — does not bleed across users")
        void scopedPerUser() {
            save(USER_ID, 10, "contrat");
            save(2L, 99, "contrat");

            Optional<Integer> max = repository.findMaxAttachIdByUserId(USER_ID);

            assertTrue(max.isPresent());
            assertEquals(10, max.get());
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndAttachId")
    class DeleteByUserIdAndAttachId {

        @Test
        @DisplayName("deletes the correct attachment")
        void deletesCorrectAttachment() {
            save(USER_ID, 1, "contrat");
            save(USER_ID, 2, "bulletin-salaire");

            repository.deleteByUserIdAndAttachId(USER_ID, 1);

            assertFalse(repository.existsById(new EmpAttachmentId(USER_ID, 1)));
            assertTrue(repository.existsById(new EmpAttachmentId(USER_ID, 2)));
        }

        @Test
        @DisplayName("is a no-op when attachment does not exist")
        void noOpWhenNotFound() {
            save(USER_ID, 1, "contrat");

            assertDoesNotThrow(() -> repository.deleteByUserIdAndAttachId(USER_ID, 999));
            assertEquals(1, repository.findByUserIdOrderByCreatedAtDesc(USER_ID).size());
        }
    }

    @Nested
    @DisplayName("findByScreen")
    class FindByScreen {

        @Test
        @DisplayName("returns all attachments matching the given screen")
        void returnsAllMatchingScreen() {
            save(USER_ID, 1, "LEAVE_REQUEST_42");
            save(USER_ID, 2, "LEAVE_REQUEST_42");
            save(2L,      1, "LEAVE_REQUEST_42");
            save(USER_ID, 3, "contrat");           // different screen

            List<EmployeeAttachment> result = repository.findByScreen("LEAVE_REQUEST_42");

            assertEquals(3, result.size());
            result.forEach(a -> assertEquals("LEAVE_REQUEST_42", a.getScreen()));
        }

        @Test
        @DisplayName("returns empty list when no attachment matches the screen")
        void returnsEmptyWhenNoMatch() {
            save(USER_ID, 1, "contrat");

            List<EmployeeAttachment> result = repository.findByScreen("LEAVE_REQUEST_99");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByScreenAndAttachId")
    class FindByScreenAndAttachId {

        @Test
        @DisplayName("returns attachment when screen and attachId both match")
        void returnsWhenFound() {
            save(USER_ID, 7, "LEAVE_REQUEST_10");

            Optional<EmployeeAttachment> result =
                    repository.findByScreenAndAttachId("LEAVE_REQUEST_10", 7);

            assertTrue(result.isPresent());
            assertEquals(7, result.get().getAttachId());
            assertEquals("LEAVE_REQUEST_10", result.get().getScreen());
        }

        @Test
        @DisplayName("returns empty when screen matches but attachId does not")
        void returnsEmptyWhenAttachIdMismatch() {
            save(USER_ID, 1, "LEAVE_REQUEST_10");

            Optional<EmployeeAttachment> result =
                    repository.findByScreenAndAttachId("LEAVE_REQUEST_10", 999);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty when attachId matches but screen does not")
        void returnsEmptyWhenScreenMismatch() {
            save(USER_ID, 1, "LEAVE_REQUEST_10");

            Optional<EmployeeAttachment> result =
                    repository.findByScreenAndAttachId("LEAVE_REQUEST_99", 1);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("composite key — EmpAttachmentId")
    class CompositeKey {

        @Test
        @DisplayName("saving same (userId, attachId) twice updates instead of inserting")
        void sameCompositeKeyUpdates() {
            save(USER_ID, 1, "contrat");
            save(USER_ID, 1, "bulletin-salaire"); // JPA merge → UPDATE

            List<EmployeeAttachment> result = repository.findByUserIdOrderByCreatedAtDesc(USER_ID);
            assertEquals(1, result.size());
            assertEquals("bulletin-salaire", result.get(0).getScreen());
        }

        @Test
        @DisplayName("same attachId on different users is allowed")
        void sameAttachIdDifferentUsers() {
            assertDoesNotThrow(() -> {
                save(1L, 1, "contrat");
                save(2L, 1, "contrat");
            });
        }
    }
}
