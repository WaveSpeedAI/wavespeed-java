package ai.wavespeed;

/**
 * The task itself reported status "failed".
 *
 * <p>This is the only error the task-level retry loop ({@code maxRetries}) re-runs:
 * the failed task is finished, so submitting again cannot duplicate running work.</p>
 */
public class WavespeedTaskFailedException extends WavespeedException {

    /**
     * Create a task-failed exception with a message.
     *
     * @param message Error message
     */
    public WavespeedTaskFailedException(String message) {
        super(message);
    }
}
