package heritage.africca.accel_connect.controller;


import heritage.africca.accel_connect.entity.Customer;
import heritage.africca.accel_connect.service.CustomerService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/customers")
@CrossOrigin("*")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService= customerService;
    }


    @GetMapping
    public List<Customer> all() {

        return customerService.list();
    }

     @GetMapping("/{id}")
        public Customer one(@PathVariable Long id) throws ChangeSetPersister.NotFoundException {
        return customerService.get(id);
    }
    @PostMapping public Customer create(@RequestBody Customer customer) {

        return customerService.create(customer);
    }
    @PutMapping("/{id}") public Customer update(@PathVariable Long id, @RequestBody Customer customer) {
            customer.setId(id);
            return customerService.update(id, customer);
    }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }



}
