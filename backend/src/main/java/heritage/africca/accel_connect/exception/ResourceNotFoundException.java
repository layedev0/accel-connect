package heritage.africca.accel_connect.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }

    public ResourceNotFoundException(String resourceName, Integer id) {
        super(resourceName + " not found with id: " + id);
    }
}
