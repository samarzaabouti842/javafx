package tn.esprit.Pidev.Models;

import java.time.LocalDateTime;

public class Prescription {
    private int id;
    private int patientId;
    private LocalDateTime dateDeb;
    private LocalDateTime dateFin;
    private String adresse;
    private String gmail;
    private String statut;
    private boolean archived;

    // Constructor for database retrieval (with ID)
    public Prescription(int id, int patientId, LocalDateTime dateDeb, LocalDateTime dateFin, String adresse, String gmail, String statut, boolean archived) {
        this.id = id;
        this.patientId = patientId;
        this.dateDeb = dateDeb;
        this.dateFin = dateFin;
        this.adresse = adresse;
        this.gmail = gmail;
        this.statut = statut;
        this.archived = archived;
    }

    // Constructor for creating new prescriptions (without ID)
    public Prescription(int patientId, LocalDateTime dateDeb, LocalDateTime dateFin, String adresse, String gmail, String statut) {
        this.patientId = patientId;
        this.dateDeb = dateDeb;
        this.dateFin = dateFin;
        this.adresse = adresse;
        this.gmail = gmail;
        this.statut = statut;
        this.archived = false;
    }

    // Default constructor
    public Prescription() {}

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public LocalDateTime getDateDeb() {
        return dateDeb;
    }

    public void setDateDeb(LocalDateTime dateDeb) {
        this.dateDeb = dateDeb;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public String toString() {
        return "Prescription{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", dateDeb=" + dateDeb +
                ", dateFin=" + dateFin +
                ", adresse='" + adresse + '\'' +
                ", gmail='" + gmail + '\'' +
                ", statut='" + statut + '\'' +
                ", archived=" + archived +
                '}';
    }
}
