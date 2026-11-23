# GestionMission — Gestion des agents et affectation aux missions

Application Java **Swing / JDBC** en architecture **N-Tiers** pour l'agence HackAcademy.

- **TP1** : gestion des missions (ajout, consultation, modification, suppression).
- **TP2** (ce dépôt) : ajout des **agents** et de leurs **affectations** aux missions. Un agent
  peut participer à plusieurs missions et une mission peut mobiliser plusieurs agents. Chaque
  affectation indique le **rôle** de l'agent et sa **date d'affectation**.

Les fonctionnalités du TP1 sont conservées.

---

## Sommaire

1. [Fonctionnalités](#fonctionnalités)
2. [Technologies et prérequis](#technologies-et-prérequis)
3. [Organisation du dépôt](#organisation-du-dépôt)
4. [Installation](#installation)
5. [Configuration de la base de données](#configuration-de-la-base-de-données)
6. [Lancement](#lancement)
7. [Jeu de données de test](#jeu-de-données-de-test)
8. [Documentation](#documentation)

---

## Fonctionnalités

| Domaine | Fonctionnalités |
|---|---|
| Missions (TP1) | Ajouter, consulter (JTable triable), modifier, supprimer une mission |
| Missions (TP2) | Nombre d'agents affectés par mission, fenêtre « agents mobilisés » |
| Agents | Lister (avec ou sans les inactifs), créer, modifier, supprimer, voir les missions d'un agent |
| Affectations | Affecter un agent à une mission (rôle + date), modifier le rôle / la date, retirer un agent, filtrer par mission |
| Qualité | Saisies contrôlées avant tout accès à la base, `SQLException` interceptées et traduites en messages clairs |

## Technologies et prérequis

| Outil | Version utilisée |
|---|---|
| JDK | 21 ou plus récent (le projet NetBeans est réglé sur la source/cible 26) |
| Apache NetBeans | version récente, projet *Java with Ant* |
| MySQL | 8.x (MariaDB 10.5+ fonctionne aussi) |
| Pilote JDBC | `mysql-connector-j-9.4.0.jar` |

## Organisation du dépôt

```
GestionMission/
├── README.md
├── sql/
│   └── gestion_missions.sql        # schéma complet + jeu de données de test
├── src/gestionmission/
│   ├── GestionMission.java         # programme de test console des DAO
│   ├── Connexion/ConnexionBDD.java # paramètres JDBC et ouverture des connexions
│   ├── Metier/                     # classes métier : Mission, Statut, Priorite, Agent, Participation
│   ├── DAO/                        # accès aux données : MissionDAO, AgentDAO, ParticipationDAO
│   └── IHM/                        # fenêtres Swing (JFrame + JDialog modales)
├── docs/
│   ├── documentation-technique.md  # architecture, MCD/MLD, choix techniques
│   ├── diagramme-classes.md        # diagrammes de classes UML
│   ├── guide-utilisateur.md        # guide illustré
│   ├── diagrammes/                 # sources Mermaid des diagrammes (.mmd)
│   └── images/                     # diagrammes exportés et captures d'écran
├── nbproject/ , build.xml , manifest.mf   # projet NetBeans (Ant)
```

## Installation

1. **Cloner le dépôt**

   ```bash
   git clone https://github.com/Skyders64/GestionMission.git
   ```

2. **Ouvrir le projet dans NetBeans** : *File → Open Project…* puis choisir le dossier
   `GestionMission`.

3. **Ajouter le pilote JDBC MySQL** (si NetBeans signale une référence manquante) :
   *clic droit sur le projet → Properties → Libraries → Classpath → Add JAR/Folder* puis
   sélectionner `mysql-connector-j-9.4.0.jar` (téléchargeable sur
   [dev.mysql.com](https://dev.mysql.com/downloads/connector/j/) ou sur Maven Central).
   Le projet référence par défaut le JAR présent sur le partage réseau de la classe.

4. **Vérifier le JDK** : *Properties → Sources → Source/Binary Format* doit correspondre au JDK
   installé (par exemple 21 si vous n'avez pas le JDK 26).

## Configuration de la base de données

1. Démarrer le serveur MySQL (WAMP, XAMPP, MySQL Server…).
2. Exécuter le script **`sql/gestion_missions.sql`** :
   - **phpMyAdmin** : onglet *Importer* puis choisir le fichier ;
   - **MySQL Workbench** : *File → Open SQL Script* puis exécuter ;
   - **ligne de commande** :

     ```bash
     mysql -u root -p < sql/gestion_missions.sql
     ```

   Le script crée la base `gestion_missions`, les tables `mission`, `agent` et `participation`
   puis insère le jeu de test. Il peut être relancé sans risque : il ne supprime rien et
   n'insère pas de doublons. Sur une base du TP1 il ajoute simplement les nouvelles tables.

3. Adapter si besoin les paramètres de connexion dans
   [`src/gestionmission/Connexion/ConnexionBDD.java`](src/gestionmission/Connexion/ConnexionBDD.java) :

   ```java
   private static final String URL =
           "jdbc:mysql://localhost:3306/gestion_missions?serverTimezone=Europe/Paris&useSSL=false&allowPublicKeyRetrieval=true";
   private static final String UTILISATEUR = "root";
   private static final String MOT_DE_PASSE = "";
   ```

## Lancement

- **Application graphique** : *Run → Run Project* (F6). La classe principale est
  `gestionmission.IHM.Menu`.
- **Test console des DAO** : clic droit sur `GestionMission.java` → *Run File* (Maj+F6).
  Le programme crée des données temporaires (agent, mission, affectation), vérifie que les
  doublons sont refusés et que la suppression en cascade fonctionne, puis nettoie la base.
- **En dehors de NetBeans** : *Clean and Build* génère `dist/GestionMission.jar` (le pilote JDBC
  est copié dans `dist/lib`) :

  ```bash
  java -jar dist/GestionMission.jar
  ```

## Jeu de données de test

| Table | Lignes | Contenu |
|---|---|---|
| `mission` | 6 | 3 missions du TP1 + 3 nouvelles (statuts et priorités variés) |
| `agent` | 6 | Cipher, NullByte, Kernel, Ghost, Phish (actifs) et Legacy (inactif) |
| `participation` | 9 | au moins un agent sur chaque mission, plusieurs agents sur 4 missions |

## Documentation

| Document | Contenu |
|---|---|
| [Documentation technique](docs/documentation-technique.md) | Architecture N-Tiers, MCD / MLD, requêtes SQL et jointures, gestion des exceptions, choix techniques |
| [Diagrammes de classes UML](docs/diagramme-classes.md) | Modèle métier et structure N-Tiers (packages IHM / DAO / Connexion) |
| [Guide utilisateur](docs/guide-utilisateur.md) | Créer un agent, l'affecter à une mission, consulter les affectations, captures d'écran légendées |
