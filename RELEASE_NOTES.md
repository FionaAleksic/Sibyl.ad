# Sibyl.ad v0.1.0-rc.4

Standalone LDAPS-Connector mit ausführbarem Java-21-JAR (Main-Class `org.sibyl.addons.ad.AdConnectorMain`). Mit `--health` kann ein autorisierter Administrator die TLS-geschützte AD-Abfrage testen und die Anzahl von Benutzern, Gruppen und Computern prüfen; `--snapshot` exportiert nach ausdrücklichem Aufruf eine JSON-Bestandsaufnahme für den späteren zentralen AD-Broker. Gruppen mit Active-Directory-Member-Range-Feldern werden jetzt paginiert statt stillschweigend abgeschnitten.

**Wichtig:** Aufruf nur auf einem genehmigten, netzwerkfähigen Connector-Host mit gültigem LDAPS-CA-Zertifikat und externem Nur-Lese-Serviceaccount. Passwörter sind ausschließlich in geschützten Servervariablen erlaubt. Die jetzige Core-Worker-Sandbox hat keinen Netzwerkzugriff; der Core startet diesen LDAPS-Connector deshalb noch nicht automatisch. AD-Schreibrechte, automatische Azubi-Rosterübernahme, regelmäßiger Sync und Credentials-Verwaltung sind weiterhin offen.
