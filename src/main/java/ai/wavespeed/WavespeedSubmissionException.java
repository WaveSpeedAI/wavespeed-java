package ai.wavespeed;

/**
 * A task submission (POST) failed and must not be retried automatically.
 *
 * <p>Submission errors are ambiguous: the server may already have created the
 * task even though no usable response was received, so the SDK never repeats
 * the submission POST on its own.</p>
 */
public class WavespeedSubmissionException extends WavespeedException {

    /**
     * Create a submission exception with a message.
     *
     * @param message Error message
     */
    public WavespeedSubmissionException(String message) {
        super(message);
    }

    /**
     * Create a submission exception with a message and a cause.
     *
     * @param message Error message
     * @param cause Underlying cause
     */
    public WavespeedSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
