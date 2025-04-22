-- Création de la table patient
CREATE TABLE IF NOT EXISTS patient (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    date_naissance DATE,
    telephone VARCHAR(20),
    adresse TEXT
);

-- Création de la table prescription
CREATE TABLE IF NOT EXISTS prescription (
    id INT PRIMARY KEY AUTO_INCREMENT,
    datedeb DATETIME NOT NULL,
    datefin DATETIME NOT NULL,
    adresse VARCHAR(255),
    Gmail VARCHAR(100),
    patientId INT,
    statut VARCHAR(50) DEFAULT 'En cours',
    FOREIGN KEY (patientId) REFERENCES patient(id)
);

-- Création de la table traitement
CREATE TABLE IF NOT EXISTS traitement (
    id INT PRIMARY KEY AUTO_INCREMENT,
    medicament VARCHAR(100) NOT NULL,
    dose VARCHAR(50) NOT NULL,
    id_p INT,
    completed BOOLEAN DEFAULT false,
    FOREIGN KEY (id_p) REFERENCES prescription(id)
);

-- Insertion de données de test
INSERT INTO patient (nom, prenom, email, password, date_naissance, telephone, adresse) VALUES
('Dupont', 'Thomas', 'thomas.dupont@email.com', 'password123', '1990-05-15', '0612345678', '123 Rue de la Santé');

INSERT INTO prescription (datedeb, datefin, adresse, Gmail, patientId, statut) VALUES
('2024-03-20 10:00:00', '2024-04-20 10:00:00', 'Cabinet Dr. Laurent', 'dr.laurent@email.com', 1, 'En cours'),
('2024-04-25 14:30:00', '2024-05-25 14:30:00', 'Clinique Centrale', 'clinique@email.com', 1, 'Planifié');

INSERT INTO traitement (medicament, dose, id_p, completed) VALUES
('Amoxicilline', '2x/jour', 1, false),
('Doliprane', 'si besoin', 1, false),
('Vitamine C', '1x/jour', 2, false); 