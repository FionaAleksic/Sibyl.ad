# Sibyl.ad v0.1.0-rc.1 – erster LDAPS-Provider (Pre-Release)

Das erste firmenneutrale AD-Integrationspaket für **Sibyl System**.

### Enthalten
- Read-only LDAPS mit Zertifikats-/Hostnameprüfung.
- AD-Benutzer und Gruppen mit paginierter Abfrage.
- Stabile AD-`objectGUID`-IDs für spätere Core-Verknüpfung.
- LDAP-Filter-Escaping, validierte Konfiguration und Zeitlimits.
- Eigenständiges Java-21-JAR mit versioniertem `module.json` und Schema.

### Sicherheit
- Kein unverschlüsselter LDAP-Bind, keine Passwörter im Paket.
- AD-Servicekonto mit minimalen Leserechten erforderlich.
- Schreiben/Ändern/Löschen in AD nicht implementiert.

### Einschränkungen
- **Pre-Release, nicht produktionsfreigegeben.** Der aktuelle Sibyl Core benötigt noch einen Addon-Loader und Identity-Sync-Service.
- Der Build enthält nur lokale Tests; ein Integrationstest gegen ein echtes freigegebenes AD steht noch aus.
- Sehr große Gruppen mit `member;range=...` werden aktuell abgelehnt, um keine unvollständigen Ergebnisse zu speichern.

GitHub-Asset `Sibyl.ad-v0.1.0-rc.1.zip` nur nach SHA-256-Verifikation verwenden.
