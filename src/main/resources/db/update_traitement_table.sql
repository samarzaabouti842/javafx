-- Ajout de la colonne completed
ALTER TABLE traitement ADD COLUMN IF NOT EXISTS completed BOOLEAN DEFAULT FALSE;

-- Ajout de la colonne statut
ALTER TABLE traitement ADD COLUMN IF NOT EXISTS statut VARCHAR(50) DEFAULT 'En cours';

-- Renommer la colonne dosage en dose (si elle existe)
-- Nous utilisons une procédure stockée pour éviter les erreurs si la colonne n'existe pas
DELIMITER //
CREATE PROCEDURE update_dosage_column()
BEGIN
    IF EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = 'masante' 
        AND TABLE_NAME = 'traitement' 
        AND COLUMN_NAME = 'dosage'
    ) THEN
        ALTER TABLE traitement CHANGE COLUMN dosage dose VARCHAR(255);
    END IF;
END //
DELIMITER ;

-- Exécuter la procédure
CALL update_dosage_column();

-- Supprimer la procédure
DROP PROCEDURE IF EXISTS update_dosage_column;

-- S'assurer que toutes les colonnes nécessaires existent avec les bons types
ALTER TABLE traitement MODIFY COLUMN id INT AUTO_INCREMENT;
ALTER TABLE traitement MODIFY COLUMN prescription_id INT NOT NULL;
ALTER TABLE traitement MODIFY COLUMN medicament VARCHAR(255) NOT NULL;
ALTER TABLE traitement MODIFY COLUMN dose VARCHAR(255) NOT NULL;
ALTER TABLE traitement MODIFY COLUMN frequence VARCHAR(255);
ALTER TABLE traitement MODIFY COLUMN duree INT;
ALTER TABLE traitement MODIFY COLUMN instructions TEXT;
ALTER TABLE traitement MODIFY COLUMN date_debut DATE;
ALTER TABLE traitement MODIFY COLUMN date_fin DATE;
ALTER TABLE traitement MODIFY COLUMN archived BOOLEAN DEFAULT FALSE;
ALTER TABLE traitement MODIFY COLUMN completed BOOLEAN DEFAULT FALSE;
ALTER TABLE traitement MODIFY COLUMN statut VARCHAR(50) DEFAULT 'En cours';

-- Ajouter les index nécessaires
ALTER TABLE traitement ADD INDEX idx_prescription_id (prescription_id);
ALTER TABLE traitement ADD INDEX idx_archived (archived);
ALTER TABLE traitement ADD INDEX idx_completed (completed);
ALTER TABLE traitement ADD INDEX idx_statut (statut);

-- Mettre à jour les contraintes de clé étrangère
ALTER TABLE traitement
ADD CONSTRAINT fk_traitement_prescription
FOREIGN KEY (prescription_id) REFERENCES prescription(id)
ON DELETE CASCADE
ON UPDATE CASCADE; 