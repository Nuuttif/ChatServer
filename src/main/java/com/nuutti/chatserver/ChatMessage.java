package com.nuutti.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ChatMessage {

    public LocalDateTime sent;
    public String user;
    public String message;

    public ChatMessage(LocalDateTime sent, String user, String message) {
        this.sent = sent;
        this.user = user;
        this.message = message;
    }

    long dateAsInt() {
        return sent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    void setSent(long epoch) {
        sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }
}

