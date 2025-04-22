package tn.esprit.Pidev.Models;

import java.time.LocalDate;

public class Traitement {
    private int id;
    private int id_p; // Foreign key to Prescription
    private String medicament;
    private String dose;
    private String description;
    private String statut;
    private LocalDate date;
    private boolean completed;
    private boolean archived;

    // Constructor for database retrieval (with ID)
    public Traitement(int id, int id_p, String medicament, String dose, String description, String statut, LocalDate date, boolean completed, boolean archived) {
        this.id = id;
        this.id_p = id_p;
        this.medicament = medicament;
        this.dose = dose;
        this.description = description;
        this.statut = statut;
        this.date = date;
        this.completed = completed;
        this.archived = archived;
    }

    // Constructor for creating new treatments (without ID)
    public Traitement(int id_p, String medicament, String dose, String description, String statut, LocalDate date) {
        this.id_p = id_p;
        this.medicament = medicament;
        this.dose = dose;
        this.description = description;
        this.statut = statut;
        this.date = date;
        this.completed = false;
        this.archived = false;
    }

    // Default constructor
    public Traitement() {}

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_p() {
        return id_p;
    }

    public void setId_p(int id_p) {
        this.id_p = id_p;
    }

    public String getMedicament() {
        return medicament;
    }

    public void setMedicament(String medicament) {
        this.medicament = medicament;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public String toString() {
        return "Traitement{" +
                "id=" + id +
                ", id_p=" + id_p +
                ", medicament='" + medicament + '\'' +
                ", dose='" + dose + '\'' +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                ", date=" + date +
                ", completed=" + completed +
                ", archived=" + archived +
                '}';
    }
}
