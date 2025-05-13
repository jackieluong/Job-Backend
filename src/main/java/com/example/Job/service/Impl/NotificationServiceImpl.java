package com.example.Job.service.Impl;

import com.example.Job.config.WebSocket.WebSocketConfig;
import com.example.Job.entity.Notification;
import com.example.Job.models.dtos.NotificationRequest;
import com.example.Job.models.dtos.NotificationResponse;
import com.example.Job.repository.NotificationRepository;
import com.example.Job.security.JwtUtil;
import com.example.Job.service.INotificationService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ModelMapper modelMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository, JwtUtil jwtUtil, SimpMessagingTemplate simpMessagingTemplate, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.jwtUtil = jwtUtil;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.modelMapper = modelMapper;
    }


    @Override
    public Notification createNotification(String recipientId, NotificationRequest notificationRequest) {


        Notification notification = new Notification();
        notification.setRecipientId(Long.valueOf(recipientId));
        notification.setMessage(notificationRequest.getMessage());
        notification.setTitle(notificationRequest.getTitle());
        notification.setLink(notificationRequest.getLink());
        notification.setCreatedAt(notificationRequest.getCreatedAt());
        try{
            return notificationRepository.save(notification);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sendNotificationToUser(String recipientId, NotificationRequest notificationRequest) {

        simpMessagingTemplate.convertAndSendToUser(recipientId,
                WebSocketConfig.userPrivateNotificationDestination,
                notificationRequest
        );

        // after send, save to db
        createNotification(recipientId, notificationRequest);

    }

    public void saveAllNotifications(List<String> recipientIDs, NotificationRequest notificationRequest) {

        System.out.println("Notification thread 1: " + Thread.currentThread().getName());

        List<Notification> notifications = recipientIDs.stream().map(recipientId -> {
            Notification notification = new Notification();
            notification.setTitle(notificationRequest.getTitle());
            notification.setMessage(notificationRequest.getMessage());
            notification.setRecipientId(Long.valueOf(recipientId));
            notification.setLink(notificationRequest.getLink());
            notification.setCreatedAt(notificationRequest.getCreatedAt());
            return notification;

        }).collect(Collectors.toList());

        notificationRepository.saveAll(notifications);

    }
    @Override
    @Async
    public void sendNotificationToMultipleUser(List<String> recipientIDs, NotificationRequest notificationRequest) {


        this.saveAllNotifications(recipientIDs, notificationRequest);

        System.out.println("Notification thread 2: " + Thread.currentThread().getName());
        recipientIDs.forEach(recipientID -> {
            simpMessagingTemplate.convertAndSendToUser(
                    recipientID,
                    WebSocketConfig.userPrivateNotificationDestination,
                    notificationRequest
                );
            }
        );

    }

    @Override
    public List<NotificationResponse> getAllNotificationsForUser() {
        Long userId = Long.valueOf(jwtUtil.extractUserIdFromToken());

        return notificationRepository.findNotificationsByRecipientId(userId).stream().map(notification ->
                modelMapper.map(notification, NotificationResponse.class)).collect(Collectors.toList());

    }

    @Override
    @Transactional
    public void markNotificationAsRead(long id) {
        notificationRepository.markAsReadById(id);
    }

}
