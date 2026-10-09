# Sibyl.ad

**Addon-ID:** `Sibyl.ad`  
**Pre-Release:** `v0.1.0-rc.1` · Java 21 · Read-only

Optionale **Active-Directory-Integration** für die firmenneutrale Plattform Sibyl System. Sibyl hat seine **eigene zentrale Datenbank und stabile Benutzer-UUIDs**; AD ist ein austauschbarer, externer Verzeichnisprovider, nicht die globale Source of Truth.

## Funktionen in v0.1.0-rc.1

- Authentifizierte **LDAPS**-Verbindung mit geprüftem TLS-Zertifikat und DNS-Hostnamen.
- Benutzer aus AD lesen: `objectGUID`, `sAMAccountName`, `userPrincipalName`, `displayName`, `mail`, Konto-aktiv-Status.
- AD-Gruppen lesen: `objectGUID`, `cn`, vollständige `member`-Liste (bei ranged results: bewusst Fehler statt abgeschnittener Daten).
- Serverseitige LDAP-Paging-Control, eindeutige GUIDs und RFC-4515-Escaping für accountbasierte Suche.
- Begrenzungen für Verbindungs-/Lesezeit, Seiten- und Ergebnismengen. Keine LDAP-Referrals zu anderen Servern.
- Read-only: **kein** Anlegen, Ändern oder Löschen von AD-Objekten. Kein unverschlüsseltes `ldap://` und keine abgeschaltete TLS-Zertifikatsprüfung.

## Installationspaket

Ein echtes GitHub **Pre-Release** enthält das Asset `Sibyl.ad-v0.1.0-rc.1.zip`, sowie `Sibyl.ad-v0.1.0-rc.1.zip.sha256`. Im ZIP:

```text
module.json
config.schema.json
sibyl-ad-0.1.0-rc.1.jar
SHA256SUMS.txt
README.md
LICENSE
```

Das Paket ist alleinstehend, keine proprietäre AD-Clientsoftware erforderlich. Es enthält keinen AD-Server und keine Account-Credentials. Der Sibyl-Core-Addon-Manager muss das Manifest lesen, die Archive-Integrität prüfen und die JAR sicher in seine Directory-Provider-Schnittstelle einbinden. **Aktuelle Grenze:** Die neue Sibyl-Core-Plugin-Runtime und das Deployment der Anwendung sind noch nicht vollständig umgesetzt. Dieses Addon ist daher noch **kein** automatisch im Live-System lauffähiger Bestandteil.

## Serverkonfiguration

Das Addon nutzt ausschließlich serverseitige Umgebungsvariablen:

| Variable | Zweck | Beispiel |
|---|---|---|
| `SIBYL_AD_URL` | AD LDAPS Host | `ldaps://dc.example.org:636` |
| `SIBYL_AD_BASE_DN` | Suchbasis | `OU=People,DC=example,DC=org` |
| `SIBYL_AD_BIND_DN` | Read-only-Serviceaccount-DN | `CN=svc-sibyl,OU=Service,DC=example,DC=org` |
| `SIBYL_AD_BIND_PASSWORD` | **Geheimes Kennwort** | Nur im Secret-Store / geschütztem Environment |
| `SIBYL_AD_PAGE_SIZE` | Paginggröße | `250` |
| `SIBYL_AD_CONNECT_TIMEOUT_MS` | Connect-Timeout | `5000` |
| `SIBYL_AD_READ_TIMEOUT_MS` | Read-Timeout | `10000` |

**Berechtigungen:** Für die Erstversion nur Lesen auf die vorgesehenen AD-Objekte delegieren (z. B. eigenes Servicekonto, minimale Rechte). Das Kennwort niemals ins Frontend, in Git, Docker-Images, Protokolle oder das ZIP schreiben. Verbindungen benötigen eine CA-vertrauenswürdige LDAPS-Konfiguration.

## Identitäten & Synchronisierung

`AdDirectoryProvider.readSnapshot()` gibt die externen Benutzer und Gruppen mit `objectGUID` zurück; `findByAccountName()` bietet die einzelne Benutzersuche. Sibyl Core entscheidet anhand `provider_id + objectGUID`, ob eine externe Identität bereits mit einer internen `user_id` (UUID) verknüpft ist. E-Mail oder Loginname alleine darf **keine** automatische Benutzerzusammenführung auslösen.

AD ist maßgeblich für AD-Objekt-ID und AD-Kontostatus; Sibyl bleibt führend für sein Rechte-/Rollensystem, Berichtshefte, Kurse, Inventar und eigene Benutzermetadaten. Passwort-Hashes werden nicht aus AD in Sibyl kopiert.

## Build & Tests

Auf einem System mit JDK 21, Python 3 und `zip`:

```bash
bash scripts/build-release.sh
```

Der Build kompiliert und führt deterministische Tests zu LDAPS-Zwang, Konfigurationsvalidierung, LDAP-Filter-Escaping und GUID-Mapping aus. Er erstellt das versionierte ZIP und eine SHA-256-Datei in `dist/`. **Für einen echten Integrationstest** müssen LDAPS-Bind, Paging und Suchergebnisse zusätzlich gegen ein explizit freigegebenes Labor-AD geprüft werden; hier werden keine produktiven AD-Zugangsdaten vorausgesetzt.

## GitHub-Release-Prozess

- Entwicklungsbranch `test` → unveränderliches **Pre-Release** `v0.1.0-rc.1`.
- `main` bleibt stabil; nicht ohne Freigabe zusammenführen.
- GitHub Actions `.github/workflows/prerelease.yml` baut/testet bei Änderungen im `test`-Branch und veröffentlicht **nur bei Erfolg**; bestehende Tags werden nicht überschrieben.
- Installationsquelle ist **ausschließlich** das veröffentlichte Release-Asset, nicht ein Branch-ZIP oder `git clone`.
- Sollte GitHub Actions gesperrt oder ausgefallen sein, bleibt die Veröffentlichung ausstehend. Dann muss der Workflow wieder freigegeben werden; ein grüner Code-Commit allein ist noch kein Release.

## Grenzen der ersten Version

- Keine AD-Schreiboperationen und keine Kerberos-/SAML-/OIDC-Authentifizierung.
- Keine Unterstützung für mehr als 50.000 Objekte in einer einzelnen Abfrage; Pagination hilft bis zu dieser Grenze.
- Noch keine Vollunterstützung für AD-Member-Range-Retrieval bei großen Gruppen; stattdessen expliziter Fehler.
- Noch keine geplanten automatischen Sync-Jobs, Web-Admin-Einstellungen, Sync-Audit oder persistente Core-Datenbank-Zuordnung – diese benötigen Core-Integration.
- Kein Versprechen einer bestandenen Live-AD-Verbindung ohne echten Test.

**Lizenz:** MIT.
