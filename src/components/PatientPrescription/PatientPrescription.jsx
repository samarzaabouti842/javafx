import React, { useState, useEffect } from 'react';
import './PatientPrescription.css';
import { FaCalendarAlt, FaPills, FaBell, FaUser } from 'react-icons/fa';

const PatientPrescription = () => {
  const [activeTab, setActiveTab] = useState('accueil');
  const [healthData, setHealthData] = useState({
    nextAppointment: {
      date: "Mercredi 23 Avril",
      doctor: "Dr. Marie Laurent",
      type: "Consultation générale"
    },
    medications: [
      { name: "Amoxicilline", dosage: "2x/jour", progress: 70 },
      { name: "Doliprane", dosage: "si besoin", progress: 100 }
    ],
    notifications: [
      { text: "Résultats disponibles", type: "info" },
      { text: "Rappel vaccination", type: "warning" }
    ],
    activityData: {
      labels: ["1 Avr", "5 Avr", "10 Avr", "15 Avr", "20 Avr", "25 Avr", "30 Avr"],
      values: [30, 45, 35, 50, 90, 40, 30]
    }
  });

  return (
    <div className="patient-prescription-container">
      <header className="header">
        <div className="logo">
          <span className="logo-icon">+</span>
          <h1>MaSanté</h1>
        </div>
        <nav>
          <a 
            href="#" 
            className={activeTab === 'accueil' ? 'active' : ''}
            onClick={() => setActiveTab('accueil')}
          >
            Accueil
          </a>
          <a 
            href="#" 
            className={activeTab === 'rendez-vous' ? 'active' : ''}
            onClick={() => setActiveTab('rendez-vous')}
          >
            Rendez-vous
          </a>
          <a 
            href="#" 
            className={activeTab === 'documents' ? 'active' : ''}
            onClick={() => setActiveTab('documents')}
          >
            Documents
          </a>
          <a 
            href="#" 
            className={activeTab === 'messages' ? 'active' : ''}
            onClick={() => setActiveTab('messages')}
          >
            Messages
          </a>
        </nav>
        <div className="profile-icon">
          <FaUser />
        </div>
      </header>

      <main className="main-content">
        <section className="welcome-section">
          <h2>Bonjour, Thomas</h2>
          <p>Voici le résumé de votre santé</p>
        </section>

        <div className="cards-container">
          {/* Carte Prochain RDV */}
          <div className="info-card">
            <div className="card-header">
              <FaCalendarAlt className="card-icon" />
              <h3>Prochain RDV</h3>
            </div>
            <div className="card-content">
              <p className="date">{healthData.nextAppointment.date}</p>
              <p className="doctor">{healthData.nextAppointment.doctor}</p>
              <p className="type">{healthData.nextAppointment.type}</p>
            </div>
          </div>

          {/* Carte Médicaments */}
          <div className="info-card">
            <div className="card-header">
              <FaPills className="card-icon" />
              <h3>Médicaments</h3>
            </div>
            <div className="card-content">
              {healthData.medications.map((med, index) => (
                <div key={index} className="medication-item">
                  <div>
                    <p className="med-name">{med.name}</p>
                    <p className="med-dosage">{med.dosage}</p>
                  </div>
                  <div className="progress-bar">
                    <div 
                      className="progress" 
                      style={{ width: `${med.progress}%` }}
                    ></div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Carte Notifications */}
          <div className="info-card">
            <div className="card-header">
              <FaBell className="card-icon" />
              <h3>Notifications</h3>
            </div>
            <div className="card-content">
              {healthData.notifications.map((notif, index) => (
                <div key={index} className="notification-item">
                  <span className={`notification-dot ${notif.type}`}></span>
                  <p>{notif.text}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Section Suivi de votre santé */}
        <section className="health-tracking-section">
          <h3>Suivi de votre santé</h3>
          <div className="activity-chart">
            {healthData.activityData.values.map((value, index) => (
              <div key={index} className="chart-bar">
                <div 
                  className="bar" 
                  style={{ height: `${value}%` }}
                ></div>
                <span className="label">{healthData.activityData.labels[index]}</span>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
};

export default PatientPrescription; 