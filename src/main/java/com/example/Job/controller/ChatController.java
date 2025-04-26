//package com.example.Job.controller;
//
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.stereotype.Controller;
//
//@Controller
//public class ChatController {
//
//    @MessageMapping("/sendMessage")
//    @SendTo("/topic/messages")
//    public String sendMessage(String message, StompHeaderAccessor headerAccessor) {
//        String username = (String) headerAccessor.getSessionAttributes().get("user");
//        return username + ": " + message;
//    }
//
//    // @MessageMapping("/ping")
//    // public void handlePing(String message) {
//    // // Chỉ cần nhận là đủ, không cần phản hồi
//    // System.out.println("Received ping: " + message);
//    // }
//}
