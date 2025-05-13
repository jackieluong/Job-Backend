package com.example.Job.config.WebSocket;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserWebSocketSession {
    private final String sessionId;
    private final Set<String> subscriptions;

    public UserWebSocketSession(String sessionId) {
        this.sessionId = sessionId;
        this.subscriptions = new ConcurrentSkipListSet<>(); // Thread-safe set
    }

    public String getSessionId() {
        return sessionId;
    }

    public Set<String> getSubscriptions() {
        return subscriptions;
    }

    public boolean addSubscription(String destination) {
        return subscriptions.add(destination); // Returns true if added, false if exists
    }

    public boolean removeSubscription(String destination) {
        return subscriptions.remove(destination); // Optional, for explicit unsubscription
    }
}
