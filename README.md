# LWJGL-PROJET-MINECRAFT

Un projet graphique 3D utilisant LWJGL (Lightweight Java Game Library) pour créer une application type Minecraft.
Référentielle :

<img width="1000" height="1000" alt="référentiel" src="https://github.com/user-attachments/assets/96717006-7e70-4033-851b-8087c81e1b5a" />

## Prérequis

- **Java JDK 11 (pas 25) ou supérieur** - [Télécharger Java](https://www.oracle.com/java/technologies/downloads/)
- **Git** - Pour cloner le projet

⚠️ **Maven n'est pas obligatoire !** Le projet inclut Maven Wrapper (`mvnw.cmd` pour Windows), qui télécharge et utilise Maven automatiquement.

## Installation et démarrage

### 0. Exécuter le code
```
mvn exec:exec     plus de exec:java !

./mvnw compile exec:exec
```

### 1. Cloner le projet
```bash
git clone <your-repo-url>
cd LWJGL_PROJET_MINECRAFT
```
OU CTRL+SHIFT+P et git clone 

### 2. Compiler et télécharger les dépendances

**⚠️ Les commandes doivent être exécutées à la racine du projet (où se trouve le `pom.xml`)**

```bash
# Sous Windows (PowerShell ou CMD)
.\mvnw.cmd clean install

# Sous Linux/Mac
./mvnw clean install
```

Maven téléchargera automatiquement toutes les dépendances du `pom.xml` (y compris LWJGL et ses natives).

### 4. Générer le JAR exécutable
```bash
# Sous Windows
mvnw.cmd package

# Sous Linux/Mac
./mvnw package
```


Pour développer et faire des push :
IL FAUT CREER UNE BRANCHE DEPUIS UNE ISSUE PUIS
SUR L'ONGLET GIT DE VSCODE CLIQUER SUR LE LOGO DE BRANCHE ET SELECTIONNER LA BRANCHE
<img width="530" height="65" alt="image" src="https://github.com/user-attachments/assets/77c5a6d3-66b0-42fb-90d6-577de112a77e" />

IL SUFFIT DE PUSH, ET POUR MERGE IL FAUT ALLER SUR GITHUB
