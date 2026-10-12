# Sibyl.ad v0.1.0-rc.2 – zentraler AD-Verzeichnisprovider

## Neu
- Read-only LDAP-Computerdatensätze mit objectGUID, Name, DNS-Hostname und Betriebssystem.
- Vollständige Nutzer-, Gruppen- und Computer-Snapshots unter einem Serviceaccount.
- Sichere, per RFC4515 escaped Suche einzelner Computer.

## Sicherheit
LDAPS und Zertifikats-/Hostnameprüfung bleiben verpflichtend. Keine Schreibfunktionen, kein Klartextpasswort im Repo; Bind-Kennwort ausschließlich via serverseitige Umgebungsvariable.

## Grenze
Einbindung in Core RC.7 noch nicht möglich: Core lädt die Pakete aktuell nur herunter, ein aktivierbarer Plugin-Lifecycle und gemeinsame Identity-API fehlen. Echte AD-Integrations-/Gruppengroßtests stehen aus.
