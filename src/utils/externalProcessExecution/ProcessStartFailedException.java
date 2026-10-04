package utils.externalProcessExecution;

public class ProcessStartFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProcessStartFailedException(String message) {
		super(message);
	}
}
