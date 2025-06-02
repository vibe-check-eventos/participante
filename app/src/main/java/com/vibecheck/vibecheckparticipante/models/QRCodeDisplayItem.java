package com.vibecheck.vibecheckparticipante.models; // Ou o pacote de sua preferência

public class QRCodeDisplayItem {
    private String eventName;
    private String eventDate;
    private String eventLocation; // Pode ser o event_address_id por enquanto
    private String qrCodeBase64; // Para passar para a FullScreenQRCode

    public QRCodeDisplayItem(String eventName, String eventDate, String eventLocation, String qrCodeBase64) {
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventLocation = eventLocation;
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

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }
}