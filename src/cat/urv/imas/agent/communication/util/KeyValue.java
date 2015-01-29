package cat.urv.imas.agent.communication.util;

/**
 * A simple key/value tuple.
 * Useful to pass pairs around.
 *
 * Created by Philipp Oliver on 30/1/15.
 */
public class KeyValue<T, O> {
    private T key;
    private O value;

    public KeyValue(T key, O value) {
        this.key = key;
        this.value = value;
    }

    public T getKey() {
        return key;
    }

    public void setKey(T key) {
        this.key = key;
    }

    public O getValue() {
        return value;
    }

    public void O(O value) {
        this.value = value;
    }
}
