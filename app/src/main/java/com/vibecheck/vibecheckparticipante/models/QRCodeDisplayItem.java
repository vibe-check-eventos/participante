package com.vibecheck.vibecheckparticipante.models;

public class QRCodeDisplayItem {
    private String eventName;
    private String eventDate;
    private String eventLocation;
    private String organizerName; // NOVO CAMPO
    private String qrCodeBase64;

    // Construtor atualizado
    public QRCodeDisplayItem(String eventName, String eventDate, String eventLocation, String organizerName, String qrCodeBase64) {
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventLocation = eventLocation;
        this.organizerName = organizerName; // Atribuir o novo campo
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public String getOrganizerName() { // NOVO GETTER
        return organizerName;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }
}