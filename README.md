# Système de Gestion des Prescriptions Médicales

Une application JavaFX pour la gestion des prescriptions médicales et des traitements.

## Fonctionnalités

- Gestion des prescriptions médicales
- Suivi des traitements
- Interface patient pour consulter les prescriptions
- Export des données en PDF et CSV
- Fonctionnalité OCR pour l'extraction de texte depuis des images/PDF

## Prérequis

- Java 11 ou supérieur
- Maven 3.6 ou supérieur
- MySQL 8.0
- Tesseract OCR (pour la fonctionnalité OCR)

## Installation

1. Cloner le repository :
```bash
git clone https://github.com/[votre-username]/prescription.git
```

2. Configurer la base de données :
- Créer une base de données MySQL nommée `masante_db`
- Exécuter le script SQL `src/main/resources/schema.sql`

3. Installer les dépendances :
```bash
mvn clean install
```

## Structure du Projet

```
src/
├── main/
│   ├── java/
│   │   └── tn/esprit/Pidev/
│   │       ├── Models/         # Classes modèles
│   │       ├── Services/       # Services métier
│   │       ├── controllers/    # Contrôleurs JavaFX
│   │       └── utils/          # Utilitaires
│   └── resources/
│       ├── fxml/              # Fichiers FXML
│       └── styles/            # Fichiers CSS
```

## Configuration

1. Base de données :
- Modifier les paramètres de connexion dans `DatabaseConnection.java`

2. Tesseract OCR :
- Installer Tesseract OCR
- Configurer le chemin vers les données de langue dans `TraitementService.java`

## Utilisation

1. Lancer l'application :
```bash
mvn javafx:run
```

2. Interface utilisateur :
- Login pour les médecins
- Interface de gestion des prescriptions
- Interface patient pour consultation

## Technologies Utilisées

- JavaFX pour l'interface graphique
- MySQL pour la base de données
- Tesseract OCR pour l'extraction de texte
- iText pour la génération de PDF
- OpenCSV pour l'export CSV

## Licence

Ce projet est sous licence MIT. 