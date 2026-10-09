package org.sibyl.addons.ad;

import java.util.List;

/**
 * A read-only source snapshot. The Sibyl Core identity API owns local UUIDs and
 * cross-provider reconciliation: the directory connector does not mutate the Core.
 */
public final class AdDirectoryProvider {
    public static final String MODULE_ID = "Sibyl.ad";
    public static final String VERSION = "0.1.0-rc.1";

    public record User(String externalId, String distinguishedName,
                       String accountName, String userPrincipalName,
                       String displayName, String email, boolean enabled) {}

    public record Group(String externalId, String distinguishedName,
                        String name, List<String> memberDistinguishedNames) {
        public Group {
            memberDistinguishedNames = List.copyOf(memberDistinguishedNames);
        }
    }

    public record Snapshot(List<User> users, List<Group> groups) {
        public Snapshot {
            users = List.copyOf(users);
            groups = List.copyOf(groups);
        }
    }

    private final AdSettings settings;

    public AdDirectoryProvider(AdSettings settings) {
        if (settings == null) throw new IllegalArgumentException("settings required");
        this.settings = settings;
    }

    /**
     * Explicit read, never an implicit write-back. The caller maps externalId
     * to a Sibyl-owned local UUID and determines field-level synchronization.
     */
    public Snapshot readSnapshot() throws Exception {
        try (AdLdapClient client = AdLdapClient.open(settings)) {
            return new Snapshot(client.readUsers(), client.readGroups());
        }
    }

    /** Single-object read with RFC 4515 escaping of untrusted account names. */
    public List<User> findByAccountName(String accountName) throws Exception {
        try (AdLdapClient client = AdLdapClient.open(settings)) {
            return client.findUsersByAccountName(accountName);
        }
    }
}
