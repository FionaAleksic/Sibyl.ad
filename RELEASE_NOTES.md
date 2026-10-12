# Sibyl.ad 0.1.0-rc.3

Wiederverwendbarer LDAPS-Verzeichnisprovider (Benutzer, Gruppen und Computer) mit `deployment.json`-Vertrag v1. `kind: dependency` erzeugt absichtlich keinen Navigationsreiter. Registrierung über Sibyl Core SDK v1 ist möglich, sobald Core RC.9 installiert ist.

**Einschränkung:** Die Registration führt noch keine echte AD-Verbindung aus und erhält keine LDAP-Schreibrechte. Für LDAPS-Laufzeitaktivierung sind ein isolierter Worker und ein durch den Administrator konfiguriertes Servicekonto notwendig.
