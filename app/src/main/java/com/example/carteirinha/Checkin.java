package com.example.carteirinha;

public class Checkin {

    private String userId;
    private String nomeAluno;
    private String tipo; // "Check-in" ou "Check-out"
    private long timestamp; // Hora da leitura
    private long qrTimestamp; // Hora da geração do QR (para evitar duplicidade)

    public Checkin() {}

    public Checkin(String userId, String nomeAluno, String tipo, long timestamp, long qrTimestamp) {
        this.userId = userId;
        this.nomeAluno = nomeAluno;
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.qrTimestamp = qrTimestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNomeAluno() { return nomeAluno; }
    public void setNomeAluno(String nomeAluno) { this.nomeAluno = nomeAluno; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getQrTimestamp() { return qrTimestamp; }
    public void setQrTimestamp(long qrTimestamp) { this.qrTimestamp = qrTimestamp; }
}