package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.entity.Customer;
import heritage.africca.accel_connect.repository.CustomerRepository;
import heritage.africca.accel_connect.service.CustomerService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository repo;
    public CustomerServiceImpl(CustomerRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Customer> list() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Customer get(Long id) throws ChangeSetPersister.NotFoundException {
        return repo.findById(id).orElseThrow(()
                -> new ChangeSetPersister.NotFoundException());}

    public Customer create(Customer customer) { customer.setId(null); return repo.save(customer); }

    public Customer update(Long id, Customer customer) { customer.setId(id); return repo.save(customer); }

    public void delete(Long id) { repo.deleteById(id); }
}
