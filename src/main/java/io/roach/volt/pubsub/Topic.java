package io.roach.volt.pubsub;

public interface Topic<E> {
    void addMessageListener(MessageListener<E> listener);

    boolean hasMessageListeners();

    void publish(Message<E> message);
}
