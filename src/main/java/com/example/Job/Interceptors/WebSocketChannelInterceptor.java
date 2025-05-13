package com.example.Job.Interceptors;

import com.example.Job.config.WebSocket.UserWebSocketSession;
import com.example.Job.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final ConcurrentHashMap<String, UserWebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;


    public WebSocketChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if(StompCommand.CONNECT.equals(accessor.getCommand())){
           return this.handleConnect(message, accessor);

        }else if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
           return this.handleSubscribe(message, accessor);
        }

        return message;

    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authToken = accessor.getFirstNativeHeader("Authorization");

        if (authToken != null && authToken.startsWith("Bearer ")) {
            String token = authToken.substring(7);

            String userId = jwtUtil.extractUserIdFromToken(token);

            if (userId == null){
                throw new RuntimeException("Invalid token");
            }
                // If user already has an active session, reject the connection

            String sessionId = accessor.getSessionId();
            // Check for existing session
            UserWebSocketSession existingSession = userSessions.get(userId);

            if (existingSession != null && !existingSession.getSessionId().equals(sessionId)) {
                log.info("New connection attempt denied for user: {} with session: {}. Active session: {}",
                        userId, sessionId, existingSession.getSessionId());
                return null; // Reject new connection
            }

            // Store new session
            userSessions.put(userId, new UserWebSocketSession(sessionId));
            log.info("User {} connected with session {}", userId, sessionId);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            // this will set the user-name with the user_id in the accessor header
            accessor.setUser(authenticationToken);

        }

        return message;
    }

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if(user == null){
            log.error("Invalid SUBSCRIBE");
            return null;
        }
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        String userId = user.getName();

        // Check for valid session and duplicate subscription
        UserWebSocketSession session = userSessions.get(userId);
        if (session == null || !session.getSessionId().equals(sessionId)) {
            log.error("No valid session for user {} in session {}", userId, sessionId);
            return null;
        }

        boolean isNewSubscription = session.addSubscription(destination);
        if (!isNewSubscription) {
            log.info("Duplicate subscription rejected for user {} to destination {} in session {}",
                    userId, destination, sessionId);
            return null; // Reject duplicate subscription
        }

        log.info("User {} subscribed to {} in session {}", userId, destination, sessionId);

        return message;
    }
    public void removeSession(String sessionId){
        // Remove the user session when disconnected
        // Remove the session and its subscriptions

        userSessions.entrySet().removeIf(entry -> {
            String userId = entry.getKey();
            UserWebSocketSession session = entry.getValue();
            if (session.getSessionId().equals(sessionId)) {
                log.info("User {} disconnected with session {}. Subscriptions: {}",
                        userId, sessionId, session.getSubscriptions());
                return true;
            }
            return false;
        });
    }

}
