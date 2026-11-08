package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.EmployeInfoPersonnelDTO;
import heritage.africca.accel_connect.dto.EmployeeDetailsRequest;
import heritage.africca.accel_connect.dto.EmployeeProfileDTO;
import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.mapper.UpcomingBirthdayMapper;
import heritage.africca.accel_connect.entity.Department;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.JobCategory;
import heritage.africca.accel_connect.entity.JobTitle;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.DepartmentRepository;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.repository.JobCategoryRepository;
import heritage.africca.accel_connect.repository.JobTitleRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeDetailsService {

    private final EmployeeDetailsRepository employeeDetailsRepository;
    private final JobTitleRepository jobTitleRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UpcomingBirthdayMapper upcomingBirthdayMapper;

    @Transactional(readOnly = true)
    public List<EmployeeDetails> findAll() {
        return employeeDetailsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public EmployeeDetails getById(Long id) {
        return employeeDetailsRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDetails", id));
    }

    @Transactional(readOnly = true)
    public EmployeInfoPersonnelDTO getEmployeInfoPersonnelByUserId(Long userId) {
        EmployeeDetails emp = employeeDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDetails not found with userId: " + userId));
        return mapToEmployeInfoPersonnelDTO(emp);
    }

    @Transactional(readOnly = true)
    public Optional<EmployeInfoPersonnelDTO> getEmployeInfoPersonnelByUserIdOptional(Long userId) {
        return employeeDetailsRepository.findByUserId(userId)
                .map(this::mapToEmployeInfoPersonnelDTO);
    }

    /**
     * Returns employee profile for display in the RH employee-profile section.
     * Uses User data (always required) and EmployeeDetails when available.
     */
    @Transactional(readOnly = true)
    public EmployeeProfileDTO getEmployeeProfileByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return employeeDetailsRepository.findByUserIdWithAssociations(userId)
                .map(emp -> mapToEmployeeProfileDTO(user, emp))
                .orElseGet(() -> mapToEmployeeProfileDTOFromUserOnly(user));
    }

    private EmployeeProfileDTO mapToEmployeeProfileDTO(User user, EmployeeDetails emp) {
        String jobTitleName = emp.getJobTitle() != null ? emp.getJobTitle().getJobTitle() : null;
        String departmentName = emp.getDepartment() != null ? emp.getDepartment().getName() : null;

        return EmployeeProfileDTO.builder()
                .id(emp.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .employeeNumber(String.format("%04d", user.getId()))
                .jobTitle(jobTitleName != null ? jobTitleName : (Boolean.TRUE.equals(user.getIntern()) ? "Stagiaire" : "Collaborateur"))
                .department(departmentName)
                .locationLabel(departmentName != null ? departmentName : "Bureau principal")
                .status(emp.getTypeContrat() != null ? emp.getTypeContrat() : "Actif")
                .phone(emp.getWorkTelephone())
                .intern(Boolean.TRUE.equals(user.getIntern()))
                .subsidyAmount(Boolean.TRUE.equals(user.getIntern()) ? 2000 : 1000)
                .joinDate(emp.getJoinedDate())
                .birthday(emp.getBirthday())
                .cni(emp.getCni())
                .sexe(emp.getSexe())
                .maritalStatus(emp.getMaritalStatus())
                .build();
    }

    private EmployeeProfileDTO mapToEmployeeProfileDTOFromUserOnly(User user) {
        return EmployeeProfileDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .employeeNumber(String.format("%04d", user.getId()))
                .jobTitle(Boolean.TRUE.equals(user.getIntern()) ? "Stagiaire" : "Collaborateur")
                .locationLabel("Bureau principal")
                .status("Actif")
                .intern(Boolean.TRUE.equals(user.getIntern()))
                .subsidyAmount(Boolean.TRUE.equals(user.getIntern()) ? 2000 : 1000)
                .build();
    }

    private EmployeInfoPersonnelDTO mapToEmployeInfoPersonnelDTO(EmployeeDetails emp) {
        EmployeInfoPersonnelDTO dto = new EmployeInfoPersonnelDTO();
        dto.setId(emp.getId());
        if (emp.getUser() != null) {
            dto.setUserId(emp.getUser().getId());
            dto.setUsername(emp.getUser().getUsername());
            dto.setEmail(emp.getUser().getEmail());
            dto.setFirstName(emp.getUser().getFirstName());
            dto.setLastName(emp.getUser().getLastName());
        }
        dto.setCni(emp.getCni());
        dto.setStatut(emp.getTypeContrat());
        dto.setTelephone(emp.getWorkTelephone());
        dto.setDateNaissance(emp.getBirthday());
        dto.setDateAdhesion(emp.getJoinedDate());
        dto.setSexe(emp.getSexe());
        dto.setMaritalStatus(emp.getMaritalStatus());
        if (emp.getDepartment() != null) {
            dto.setDepartmentId(emp.getDepartment().getId());
            dto.setDepartmentName(emp.getDepartment().getName());
        }
        if (emp.getJobTitle() != null) {
            dto.setJobTitleId(emp.getJobTitle().getId());
            dto.setJobTitleName(emp.getJobTitle().getJobTitle());
        }
        if (emp.getJobCategory() != null) {
            dto.setJobCategoryId(emp.getJobCategory().getId());
            dto.setJobCategoryName(emp.getJobCategory().getName());
            dto.setJobCategoryCode(emp.getJobCategory().getCategoryCode());
            dto.setJobCategorySalaireBase(emp.getJobCategory().getSalaireBase());
        }
        return dto;
    }

    @Transactional
    public EmployeeDetails create(EmployeeDetailsRequest request) {
        JobTitle jobTitle = resolveJobTitle(request.getJobTitleId());
        JobCategory jobCategory = resolveJobCategory(request.getJobCategoryId());
        Department department = resolveDepartment(request.getDepartmentId());

        EmployeeDetails entity = new EmployeeDetails();
        mapRequestToEntity(request, entity, jobTitle, jobCategory, department);
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
            entity.setUser(user);
        }

        EmployeeDetails saved = employeeDetailsRepository.save(entity);
        log.info("EmployeeDetails created with id: {}", saved.getId());
        return saved;
    }

    /**
     * Creates a new employee or updates an existing one.
     * If request.id is provided and the employee exists, updates it; otherwise creates a new one.
     */
    @Transactional
    public EmployeeDetails createOrUpdate(EmployeeDetailsRequest request) {
        if (request.getId() != null) {
            return employeeDetailsRepository.findById(request.getId())
                    .map(existing -> update(existing.getId(), request))
                    .orElseGet(() -> create(request));
        }
        return create(request);
    }

    @Transactional
    public EmployeeDetails update(Long id, EmployeeDetailsRequest request) {
        EmployeeDetails existing = employeeDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDetails", id));

        JobTitle jobTitle = resolveJobTitle(request.getJobTitleId());
        JobCategory jobCategory = resolveJobCategory(request.getJobCategoryId());
        Department department = resolveDepartment(request.getDepartmentId());

        mapRequestToEntity(request, existing, jobTitle, jobCategory, department);
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
            existing.setUser(user);
        } else {
            existing.setUser(null);
        }

        EmployeeDetails saved = employeeDetailsRepository.save(existing);
        log.info("EmployeeDetails updated with id: {}", id);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        EmployeeDetails existing = employeeDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDetails", id));
        employeeDetailsRepository.delete(existing);
        log.info("EmployeeDetails deleted with id: {}", id);
    }

    private JobTitle resolveJobTitle(Long jobTitleId) {
        if (jobTitleId == null) return null;
        return jobTitleRepository.findById(jobTitleId)
                .orElseThrow(() -> new ResourceNotFoundException("JobTitle", jobTitleId));
    }

    private JobCategory resolveJobCategory(Long jobCategoryId) {
        if (jobCategoryId == null) return null;
        return jobCategoryRepository.findById(jobCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("JobCategory", jobCategoryId));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) return null;
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
    }

    private void mapRequestToEntity(EmployeeDetailsRequest request, EmployeeDetails entity,
                                    JobTitle jobTitle, JobCategory jobCategory, Department department) {
        entity.setBirthday(request.getBirthday());
        entity.setSexe(request.getSexe());
        entity.setMaritalStatus(request.getMaritalStatus());
        entity.setCni(request.getCni() != null ? request.getCni().trim() : null);
        entity.setJobTitle(jobTitle);
        entity.setJobCategory(jobCategory);
        entity.setDepartment(department);
        entity.setWorkTelephone(request.getWorkTelephone() != null ? request.getWorkTelephone().trim() : null);
        entity.setJoinedDate(request.getJoinedDate());
        entity.setTypeContrat(request.getTypeContrat() != null ? request.getTypeContrat().trim() : null);

        // Keep the day/month of birth in sync with the full birthday so upcoming
        // birthdays can be queried without depending on the birth year.
        LocalDate birthday = request.getBirthday();
        entity.setDayBirth(birthday != null ? birthday.getDayOfMonth() : null);
        entity.setMonthBirth(birthday != null ? birthday.getMonthValue() : null);
    }

    /**
     * Returns employees whose birthday falls within the next {@code windowDays}
     * days (0 = today), ordered by the soonest birthday first. Based on the
     * stored day/month of birth, so it is independent of the birth year.
     */
    @Transactional(readOnly = true)
    public List<UpcomingBirthdayDTO> getUpcomingBirthdays(int windowDays) {
        LocalDate today = LocalDate.now();
        return employeeDetailsRepository.findBirthdayCandidates().stream()
                .map(emp -> upcomingBirthdayMapper.toDto(emp, today))
                .filter(dto -> dto.getNextBirthday() != null)
                .filter(dto -> dto.getDaysUntil() <= windowDays)
                .sorted(Comparator.comparingInt(UpcomingBirthdayDTO::getDaysUntil))
                .collect(Collectors.toList());
    }
}
