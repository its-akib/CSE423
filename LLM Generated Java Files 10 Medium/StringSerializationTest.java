
package org.keycloak.common.util;

import org.keycloak.common.util.StringSerialization.Deserializer;

import java.net.URI;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests validating {@link StringSerialization}'s serialize/deserialize round trip
 * across plain strings, nulls, separator-containing strings, and non-String types.
 */
public class StringSerializationTest {

    @Test
    public void testString() {
        String a = "alpha";
        String b = "beta:gamma";
        String c = null;
        String d = "delta";
        String e = "epsilon";

        String serialized = StringSerialization.serialize(a, b, c, d, e);
        Deserializer deserializer = StringSerialization.deserialize(serialized);

        assertThat(deserializer.next(String.class), is(a));
        assertThat(deserializer.next(String.class), is(b));
        assertThat(deserializer.next(String.class), nullValue());
        assertThat(deserializer.next(String.class), is(d));
        assertThat(deserializer.next(String.class), is(e));
        assertThat(deserializer.next(String.class), nullValue());
    }

    @Test
    public void testStringWithSeparators() {
        String a = "one;two";
        String b = "three";
        String c = "four;five;six";
        String d = "seven";
        String e = "eight;";

        String serialized = StringSerialization.serialize(a, b, c, d, e);
        Deserializer deserializer = StringSerialization.deserialize(serialized);

        assertThat(deserializer.next(String.class), is(a));
        assertThat(deserializer.next(String.class), is(b));
        assertThat(deserializer.next(String.class), is(c));
        assertThat(deserializer.next(String.class), is(d));
        assertThat(deserializer.next(String.class), is(e));
        assertThat(deserializer.next(String.class), nullValue());
    }

    @Test
    public void testStringUri() {
        String a = "alpha";
        String b = "beta";
        URI c = URI.create("http://my.domain.com");
        String d = "delta";
        String e = "epsilon";

        String serialized = StringSerialization.serialize(a, b, c, d, e);
        Deserializer deserializer = StringSerialization.deserialize(serialized);

        assertThat(deserializer.next(String.class), is(a));
        assertThat(deserializer.next(String.class), is(b));
        assertThat(deserializer.next(URI.class), is(c));
        assertThat(deserializer.next(String.class), is(d));
        assertThat(deserializer.next(String.class), is(e));
        assertThat(deserializer.next(String.class), nullValue());
    }
}
