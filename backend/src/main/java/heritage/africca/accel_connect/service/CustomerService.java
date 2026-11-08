package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.Customer;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.util.List;

public interface CustomerService {

    List<Customer> list();
    Customer get(Long id) throws ChangeSetPersister.NotFoundException;
    Customer create(Customer customer);
    Customer update(Long id, Customer customer);
    void delete(Long id);
}
