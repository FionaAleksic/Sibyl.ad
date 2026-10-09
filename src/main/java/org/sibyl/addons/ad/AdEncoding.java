package org.sibyl.addons.ad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/** Protocol helpers exposed for identity mapping and connector unit tests. */
public final class AdEncoding {
    private AdEncoding() {}

    /** RFC 4515: escape all special characters and control / non-ASCII bytes. */
    public static String escapeFilter(String value) {
        if (value == null) throw new IllegalArgumentException("LDAP value cannot be null");
        StringBuilder result = new StringBuilder(value.length());
        for (byte b : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            int ch = b & 0xff;
            if (ch == '*' || ch == '(' || ch == ')' || ch == '\\' || ch == 0 || ch < 0x20 || ch >= 0x7f) {
                result.append('\\');
                result.append(Character.forDigit((ch >> 4) & 0xf, 16));
                result.append(Character.forDigit(ch & 0xf, 16));
            } else {
                result.append((char) ch);
            }
        }
        return result.toString();
    }

    /** Microsoft's binary objectGUID encodes the first three UUID groups little-endian. */
    public static String objectGuid(byte[] value) {
        if (value == null || value.length != 16) throw new IllegalArgumentException("objectGUID must be exactly 16 bytes");
        ByteBuffer guid = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        long a = Integer.toUnsignedLong(guid.getInt());
        long b = Short.toUnsignedLong(guid.getShort());
        long c = Short.toUnsignedLong(guid.getShort());
        long upper = (a << 32) | (b << 16) | c;
        guid.order(ByteOrder.BIG_ENDIAN);
        long lower = guid.getLong();
        return new UUID(upper, lower).toString();
    }
}
