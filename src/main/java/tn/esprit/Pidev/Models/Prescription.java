package tn.esprit.Pidev.Models;

import java.time.LocalDate;

public class Prescription {
    private int id;
    private int patientId;
    private LocalDate dateDeb;
    private LocalDate dateFin;
    private String adresse;
    private String gmail;
    private String statut;
    private boolean archived;

    public Prescription() {
    }

    public Prescription(int id, int patientId, LocalDate dateDeb, LocalDate dateFin, String adresse, String gmail, String statut, boolean archived) {
        this.id = id;
        this.patientId = patientId;
        this.dateDeb = dateDeb;
        this.dateFin = dateFin;
        this.adresse = adresse;
        this.gmail = gmail;
        this.statut = statut;
        this.archived = archived;
    }

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

    public LocalDate getDateDeb() {
        return dateDeb;
    }

    public void setDateDeb(LocalDate dateDeb) {
        this.dateDeb = dateDeb;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
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