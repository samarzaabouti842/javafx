-- Création de la base de données
CREATE DATABASE IF NOT EXISTS masante_db;
USE masante_db;

-- Table des patients
CREATE TABLE IF NOT EXISTS patients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE,
    email VARCHAR(255),
    telephone VARCHAR(20)
);

-- Table des prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT,
    date_prescription DATE NOT NULL,
    notes TEXT,
    statut VARCHAR(50) DEFAULT 'En cours',
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- Table des traitements
CREATE TABLE IF NOT EXISTS traitements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    prescription_id INT,
    medicament VARCHAR(255) NOT NULL,
    dose VARCHAR(100),
    frequence VARCHAR(100),
    duree INT,
    instructions TEXT,
    date_debut DATE,
    statut VARCHAR(50) DEFAULT 'En cours',
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id)
);

-- Insertion de données de test
INSERT INTO patients (nom, prenom, date_naissance, email, telephone) VALUES
('Dupont', 'Jean', '1980-01-15', 'jean.dupont@email.com', '0123456789'),
('Martin', 'Marie', '1992-05-20', 'marie.martin@email.com', '0987654321'); 