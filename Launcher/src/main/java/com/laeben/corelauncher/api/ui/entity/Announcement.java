package com.laeben.corelauncher.api.ui.entity;

public record Announcement(String title, String content, AnnouncementType type) {
    public enum AnnouncementType {
        BROADCAST, ERROR, INFO, GAME
    }
}
