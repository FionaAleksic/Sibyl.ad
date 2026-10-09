package org.sibyl.addons.ad;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Zero-dependency deterministic tests. Requires only JDK 21 and a checked out repository.
 * Does not connect to AD; LDAPS integration must be tested against a controlled lab AD.
 */
public final class AdConnectorTests {
    private static int checks;

    private static void ok(boolean expected, String description) {
        checks++;
        if (!expected) throw new AssertionError(description);
    }

    private static void rejects(Runnable code, String description) {
        checks++;
        try {
            code.run();
        } catch (IllegalArgumentException ex) {
            return;
        }
        throw new AssertionError(description);
    }

    public static void main(String[] args) {
        AdSettings valid = new AdSettings(
            URI.create("ldaps://ad.example.org:636"),
            "OU=Employees,DC=example,DC=org",
            "CN=sibyl-reader,OU=Services,DC=example,DC=org",
            250, 5000, 10000
        );
        ok("ldaps".equals(valid.serverUrl().getScheme()), "LDAPS accepted");
        rejects(() -> new AdSettings(URI.create("ldap://ad.example.org:389"), valid.baseDn(),
            valid.bindDn(), 250, 5000, 10000), "Cleartext LDAP forbidden");
        rejects(() -> new AdSettings(URI.create("ldaps://user:password@ad.example.org"), valid.baseDn(),
            valid.bindDn(), 250, 5000, 10000), "Credentials in URL forbidden");
        rejects(() -> new AdSettings(URI.create("ldaps://ad.example.org/untrusted"), valid.baseDn(),
            valid.bindDn(), 250, 5000, 10000), "URL path forbidden");
        rejects(() -> new AdSettings(URI.create("ldaps://ad.example.org"), "OU=Stuff", valid.bindDn(),
            250, 5000, 10000), "Invalid Base DN forbidden");
        rejects(() -> new AdSettings(valid.serverUrl(), valid.baseDn(), valid.bindDn(),
            1001, 5000, 10000), "Oversized page forbidden");

        String ldap = AdEncoding.escapeFilter("a*(b)" + "\\" + (char) 0);
        ok("a\\2a\\28b\\29\\5c\\00".equals(ldap), "RFC4515 chars");
        ok("x\\c3\\a4".equals(AdEncoding.escapeFilter("xä")), "UTF-8 byte escaping");
        rejects(() -> AdEncoding.escapeFilter(null), "Null LDAP value forbidden");

        byte[] guid = {
            (byte)0x33,(byte)0x22,(byte)0x11,(byte)0x00,
            (byte)0x55,(byte)0x44,(byte)0x77,(byte)0x66,
            (byte)0x88,(byte)0x99,(byte)0xaa,(byte)0xbb,
            (byte)0xcc,(byte)0xdd,(byte)0xee,(byte)0xff
        };
        ok("00112233-4455-6677-8899-aabbccddeeff".equals(AdEncoding.objectGuid(guid)), "objectGUID little endian");
        rejects(() -> AdEncoding.objectGuid(new byte[3]), "Wrong GUID length forbidden");

        ok("Sibyl.ad".equals(AdDirectoryProvider.MODULE_ID), "Stable addon id");
        ok("0.1.0-rc.1".equals(AdDirectoryProvider.VERSION), "Release version");
        AdDirectoryProvider.User user = new AdDirectoryProvider.User(
            "guid", "CN=Person,DC=example,DC=org", "account", "person@example.org", "Person", "person@example.org", true
        );
        ok(user.enabled() && "guid".equals(user.externalId()), "Read-only user record");
        AdDirectoryProvider.Group group = new AdDirectoryProvider.Group(
            "group-id", "CN=Group,DC=example,DC=org", "Group", java.util.List.of("CN=Person,DC=example,DC=org")
        );
        ok(group.memberDistinguishedNames().size() == 1, "Group membership snapshot");

        System.out.println("All " + checks + " AD connector checks passed.");
    }
}
