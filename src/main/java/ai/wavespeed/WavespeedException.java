package ai.wavespeed;

/**
 * Base unchecked exception for errors raised by the WaveSpeed Java SDK.
 */
public class WavespeedException extends RuntimeException {

    /**
     * Create an exception with a message.
     *
     * @param message Error message
     */
    public WavespeedException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and a cause.
     *
     * @param message Error message
     * @param cause Underlying cause
     */
    public WavespeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
