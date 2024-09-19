package com.example.login;

import java.util.List;

public class Booking {
    private List<String> device; // Thay đổi thành List<String>
    private String startTime;
    private String endTime;
    private long price;

    public Booking() {
        // Default constructor required for calls to DataSnapshot.getValue(Booking.class)
    }

    public Booking(List<String> device, String startTime, String endTime, long price) {
        this.device = device;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
    }

    // Getters and setters
    public List<String> getDevice() {
        return device;
    }

    public void setDevice(List<String> device) {
        this.device = device;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }
}
