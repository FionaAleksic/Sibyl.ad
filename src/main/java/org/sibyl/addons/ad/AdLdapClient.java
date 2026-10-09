package org.sibyl.addons.ad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

/**
 * LDAP read-only collector. All network access requires authenticated LDAPS
 * with normal JVM CA trust / host verification. This class does not expose
 * create/update/delete operations. No real AD server is needed for unit tests.
 */
final class AdLdapClient implements AutoCloseable {
    private static final int MAX_ENTRIES = 50_000;
    private static final String[] USER_ATTRS = {
        "objectGUID", "sAMAccountName", "userPrincipalName",
        "displayName", "mail", "userAccountControl"
    };
    private static final String[] GROUP_ATTRS = {
        "objectGUID", "cn", "member"
    };
    private final LdapContext context;
    private final AdSettings settings;

    private AdLdapClient(LdapContext context, AdSettings settings) {
        this.context = context;
        this.settings = settings;
    }

    static AdLdapClient open(AdSettings settings) throws NamingException {
        if (Boolean.getBoolean("com.sun.jndi.ldap.object.disableEndpointIdentification")) {
            throw new IllegalStateException("LDAPS endpoint verification must not be disabled");
        }
        String password = System.getenv("SIBYL_AD_BIND_PASSWORD");
        if (password == null || password.isEmpty())
            throw new IllegalStateException("Missing server-side SIBYL_AD_BIND_PASSWORD");
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, settings.serverUrl().toString());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, settings.bindDn());
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.REFERRAL, "throw"); // no implicit binding to other servers
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(settings.connectTimeoutMs()));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(settings.readTimeoutMs()));
        // LDAPS protects the bind password in transit. No plaintext ldap:// fallback.
        return new AdLdapClient(new InitialLdapContext(env, null), settings);
    }

    List<AdDirectoryProvider.User> readUsers() throws Exception {
        return collect("(&(objectCategory=person)(objectClass=user))", USER_ATTRS,
            (dn, a) -> new AdDirectoryProvider.User(
                guid(a), dn, str(a, "sAMAccountName"), str(a, "userPrincipalName"),
                str(a, "displayName"), str(a, "mail"),
                (intAttr(a, "userAccountControl") & 0x2) == 0
            ));
    }

    List<AdDirectoryProvider.User> findUsersByAccountName(String account) throws Exception {
        if (account == null || account.isBlank() || account.length() > 256)
            throw new IllegalArgumentException("Invalid account name");
        String safe = AdEncoding.escapeFilter(account);
        return collect("(&(objectCategory=person)(objectClass=user)(sAMAccountName=" + safe + "))",
            USER_ATTRS, (dn, a) -> new AdDirectoryProvider.User(
                guid(a), dn, str(a, "sAMAccountName"), str(a, "userPrincipalName"),
                str(a, "displayName"), str(a, "mail"),
                (intAttr(a, "userAccountControl") & 0x2) == 0
            ));
    }

    List<AdDirectoryProvider.Group> readGroups() throws Exception {
        return collect("(&(objectCategory=group)(objectClass=group))", GROUP_ATTRS,
            (dn, attributes) -> new AdDirectoryProvider.Group(
                guid(attributes), dn, str(attributes, "cn"),
                members(attributes)
            ));
    }

    @FunctionalInterface
    private interface Converter<T> { T convert(String distinguishedName, Attributes attributes) throws Exception; }

    private <T> List<T> collect(String filter, String[] attrs, Converter<T> mapper) throws Exception {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(attrs);
        controls.setTimeLimit(settings.readTimeoutMs());
        controls.setCountLimit(0); // AD paging and explicit hard limit below
        List<T> result = new ArrayList<>();
        byte[] cookie = null;
        do {
            context.setRequestControls(new Control[]{new PagedResultsControl(settings.pageSize(), cookie, Control.CRITICAL)});
            NamingEnumeration<SearchResult> entries = context.search(settings.baseDn(), filter, controls);
            try {
                while (entries.hasMore()) {
                    if (result.size() >= MAX_ENTRIES)
                        throw new IOException("Directory result exceeded safety limit: " + MAX_ENTRIES);
                    SearchResult entry = entries.next();
                    result.add(mapper.convert(entry.getNameInNamespace(), entry.getAttributes()));
                }
            } finally {
                entries.close();
            }
            cookie = null;
            Control[] responseControls = context.getResponseControls();
            boolean paginationAcknowledged = false;
            if (responseControls != null) {
                for (Control c : responseControls) {
                    if (c instanceof PagedResultsResponseControl page) {
                        paginationAcknowledged = true;
                        cookie = page.getCookie();
                    }
                }
            }
            if (!paginationAcknowledged)
                throw new IOException("Server did not acknowledge critical LDAP paging control");
        } while (cookie != null && cookie.length > 0);
        return List.copyOf(result);
    }

    private static String guid(Attributes attributes) throws NamingException {
        Attribute field = attributes.get("objectGUID");
        if (field == null || !(field.get() instanceof byte[] bytes)) {
            throw new NamingException("objectGUID missing or not in binary form");
        }
        return AdEncoding.objectGuid(bytes);
    }

    private static String str(Attributes attributes, String key) throws NamingException {
        Attribute field = attributes.get(key);
        Object value = field == null ? null : field.get();
        return value == null ? "" : value.toString();
    }

    private static int intAttr(Attributes attributes, String key) throws NamingException {
        String value = str(attributes, key);
        if (value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            NamingException wrapped = new NamingException("Invalid LDAP integer attribute: " + key);
            wrapped.initCause(ex);
            throw wrapped;
        }
    }

    private static List<String> members(Attributes attributes) throws NamingException {
        // AD returns ranged member attributes for big groups. Failing closed
        // prevents silent partial synchronization until range retrieval ships.
        NamingEnumeration<? extends Attribute> all = attributes.getAll();
        try {
            while (all.hasMore()) {
                String key = all.next().getID().toLowerCase(Locale.ROOT);
                if (key.startsWith("member;range="))
                    throw new NamingException("AD ranged group memberships require a later connector version");
            }
        } finally {
            all.close();
        }
        Attribute groupMembers = attributes.get("member");
        List<String> values = new ArrayList<>();
        if (groupMembers != null) {
            NamingEnumeration<?> enumeration = groupMembers.getAll();
            try {
                while (enumeration.hasMore()) {
                    if (values.size() >= MAX_ENTRIES)
                        throw new NamingException("Group member list exceeded safety limit");
                    values.add(enumeration.next().toString());
                }
            } finally {
                enumeration.close();
            }
        }
        return List.copyOf(values);
    }

    @Override
    public void close() throws NamingException {
        context.close();
    }
}
