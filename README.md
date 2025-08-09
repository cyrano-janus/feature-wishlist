# Feature Wishlist (Spring Boot + Vaadin)

Eine schlanke App, um Feature-Wünsche zu sammeln, zu **priorisieren** (Voting) und ihren **Status** zu verwalten – gebaut mit **Spring Boot 3**, **Vaadin 24**, **JPA/H2**. Bereit für **Cloud Foundry** und unter Berücksichtigung der **12-Factor-Prinzipien**.

> Demo-Logins: `admin/admin`, `user/user`  
> Startseite: `http://localhost:8080`  •  H2-Konsole: `http://localhost:8080/h2`

---

## 🚀 Features

- ✍️ **Features einreichen** (Titel, Beschreibung, Kategorie) – als **Dialog/Popup**
- 🗳️ **Voting** pro Feature (1 Vote pro Nutzer/Browser)
- 📊 **Übersicht/Filter** nach Status, Datum, Kategorie
- 🔧 **Status bearbeiten** (z. B. Offen → In Arbeit) **nur für Admin**
- 🔐 **Rollen & Login** (USER/ADMIN) via Spring Security
- ♿ **Barrierefreiheit**: Tastatur, Fokus, ARIA (Vaadin-Komponenten)

---

## 🛠 Projekthinweise

- **IDE**: Entwicklung in **VS Code Studio** (empfohlen mit Lombok-Plugin und Java Extension Pack)  
- **Versionierung**: Code wird in **GitLab** gespeichert  
- **Architekturprinzipien**: Umsetzung nach den **[12-Factor App](https://12factor.net/)** Richtlinien  
- **Deployment**: Produktivbetrieb erfolgt auf **Cloud Foundry** (Deployment-Beispiel siehe unten)  
- **Datenbanken**:  
  - Entwicklung: **H2 In-Memory DB** (auto-reset beim Neustart)  
  - Produktion: **PostgreSQL** oder andere relationale DB  
- **UI**: Lizenzfrei, umgesetzt mit **Vaadin Flow**  
- **Barrierefreiheit**: Umsetzung nach **WCAG**-Standards (Tastaturnavigation, Screenreader, Kontraste)

---

## 🧰 Tech-Stack

- **Java 17**, **Spring Boot 3.2.x**
- **Vaadin 24.3.x** (Flow)
- **JPA/Hibernate**
- **H2** (Dev) • **PostgreSQL** (Prod empfohlen)
- **Lombok**
- **Spring Security**

---

## ⚡ Schnellstart (lokal)

```bash
# 1) Repository klonen
git clone <DEIN-REPO.git>
cd feature-wishlist

# 2) Starten
mvn spring-boot:run

# 3) Öffnen
# App:      http://localhost:8080
# H2-DB:    http://localhost:8080/h2   (JDBC URL: jdbc:h2:mem:testdb / user: sa)
```

### Standard-Logins

| Benutzer | Passwort | Rolle  |
|---------:|:--------:|:------:|
| admin    | admin    | ADMIN  |
| user     | user     | USER   |

---

## 🗂️ Projektstruktur

```
src/main/java/com/example/featurewishlist
├── FeatureWishlistApplication.java
├── config
│   └── SecurityConfig.java
├── model
│   ├── FeatureRequest.java
│   ├── FeatureStatus.java
│   └── Vote.java
├── repository
│   ├── FeatureRequestRepository.java
│   └── VoteRepository.java
└── view
    └── FeatureListView.java
```

---

## 🧩 Architekturüberblick

**Domainmodell (vereinfacht)**

```
User (Spring Security, InMemory)
        └─(vote)──► Vote ──► FeatureRequest
                            ▲
                       status/category/createdAt
```

- `FeatureRequest` – der Wunsch (Titel, Beschreibung, Kategorie, Status)
- `Vote` – Zuordnung (Feature, voterId/Cookie)
- `SecurityConfig` – Rollen, Login, URL-Schutz

---

## ⚙️ Konfiguration

`src/main/resources/application.properties` (Dev-Defaults):

```properties
server.port=8080

# H2
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2

# Vaadin
vaadin.launch-browser=true
```

**12-Factor Tipps**
- Konfiguration über **Umgebungsvariablen** (DB-URL, Credentials, Feature-Flags)
- Logs auf **STDOUT**
- **Stateless**: keine Session-gebundenen Zustände im Server (Votes identifizieren wir per Cookie; bei echten Usern per User-ID)

---

## ☁ Cloud Foundry (Beispiel)

`manifest.yml`:

```yaml
applications:
  - name: feature-wishlist
    memory: 1G
    instances: 1
    path: target/feature-wishlist-0.0.1-SNAPSHOT.jar
    env:
      JBP_CONFIG_SPRING_AUTO_RECONFIGURATION: '{enabled: true}'
      JAVA_OPTS: '-Xmx512m'
      # DB-Config via Services/Broker:
      SPRING_DATASOURCE_URL: ${vcap.services.postgres.credentials.jdbcUrl}
```

> Prod: PostgreSQL-Service binden und `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` setzen.  
> Build: `mvn -DskipTests package`

---

## ✅ Rollenbasierte UI

- **USER**: Features einreichen, voten
- **ADMIN**: zusätzlich Status ändern im Grid

> Hinweis: Für echte Userkonten → DB-gestützte User/Passwörter (JPA) oder OAuth2/OpenID (Keycloak, Azure AD, …).

---

## 🧪 Qualität & Barrierefreiheit

- **A11y-Checks**: Tastaturnavigation, Fokus-Reihenfolge, Kontrast
- Test-Tools: axe DevTools, WAVE
- **CI-Idee**: Linting/Build über GitHub Actions/GitLab CI

Beispiel `.github/workflows/maven.yml`:

```yaml
name: build
on: [push, pull_request]
jobs:
  maven:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Build with Maven
        run: mvn -B -DskipTests package
```

---

## 🧯 Troubleshooting

- **`javax.servlet.* nicht vorhanden`** → Spring Boot 3 nutzt **`jakarta.*`** (Cookie: `jakarta.servlet.http.Cookie`)
- **Vaadin-Artifakt nicht gefunden** → stabile Version nutzen (z. B. `24.3.x`)
- **H2-Konsole** geht nicht → URL prüfen: `/h2`, JDBC: `jdbc:h2:mem:testdb`

---

## 📌 Roadmap

- Sortierung nach Votes
- Kommentare pro Feature
- DB-User (JPA) statt InMemory
- E-Mail/Benachrichtigungen
- Audit/History (Statuswechsel)

---

## 📜 Lizenz

MIT (oder firmenspezifisch – bitte anpassen)

---

## ♻ GitHub/GitLab einchecken (Quickstart)

```bash
# im Projektordner
git init
git add .
git commit -m "feat: initial feature-wishlist with voting & admin roles"
git branch -M main
git remote add origin https://github.com/<DEIN-USER>/feature-wishlist.git
git push -u origin main
```

> Für **GitLab** analog:  
> `git remote add origin https://gitlab.com/<DEIN-USER>/feature-wishlist.git`
