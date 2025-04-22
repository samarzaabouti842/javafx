-- Création de la base de données
CREATE DATABASE IF NOT EXISTS pidev;
USE pidev;

-- Création de la table traitement
CREATE TABLE IF NOT EXISTS traitement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    medicament VARCHAR(100) NOT NULL,
    dose DOUBLE NOT NULL,
    description TEXT,
    prescription_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Création de la table prescription
CREATE TABLE IF NOT EXISTS prescription (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date_prescription DATE NOT NULL,
    medecin_id INT NOT NULL,
    patient_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Ajout d'une clé étrangère pour prescription_id
ALTER TABLE traitement
ADD CONSTRAINT fk_traitement_prescription
FOREIGN KEY (prescription_id) REFERENCES prescription(id)
ON DELETE CASCADE; 