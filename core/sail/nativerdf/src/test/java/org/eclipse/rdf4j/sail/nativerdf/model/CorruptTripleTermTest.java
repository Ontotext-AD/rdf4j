package org.eclipse.rdf4j.sail.nativerdf.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

public class CorruptTripleTermTest {

	private static CorruptTripleTerm tripleWithData(byte[] data) {
		return new CorruptTripleTerm(null, 789, data);
	}

	@Test
	public void recoversUtf8OrAscii() {
		byte[] invalid1 = new byte[] { (byte) 0xC3, (byte) 0x28 };
		byte[] valid = "validlong".getBytes(StandardCharsets.UTF_8);
		byte[] invalid2 = new byte[] { (byte) 0xC0, (byte) 0xAF };
		byte[] tail = "abc".getBytes(StandardCharsets.UTF_8);

		byte[] data = new byte[invalid1.length + valid.length + invalid2.length + tail.length];
		int pos = 0;
		System.arraycopy(invalid1, 0, data, pos, invalid1.length);
		pos += invalid1.length;
		System.arraycopy(valid, 0, data, pos, valid.length);
		pos += valid.length;
		System.arraycopy(invalid2, 0, data, pos, invalid2.length);
		pos += invalid2.length;
		System.arraycopy(tail, 0, data, pos, tail.length);

		CorruptTripleTerm triple = tripleWithData(data);
		String stringValue = triple.stringValue();

		assertTrue(stringValue.startsWith("CorruptTripleTerm_with_ID_789: "));
		assertTrue(stringValue.contains("CorruptTripleTerm with ID 789 with possible data: "));
		assertTrue(stringValue.contains("validlong"), "Should recover core decodable region");
	}

	@Test
	public void fallsBackToHexWhenNoDecodable() {
		byte[] body = new byte[] { (byte) 0x80, (byte) 0x81, (byte) 0xFE, (byte) 0xFF };
		CorruptTripleTerm triple = tripleWithData(body);
		String stringValue = triple.stringValue();
		assertTrue(stringValue.contains(Hex.encodeHexString(body)), "Should include hex fallback");
	}

	@Test
	public void stopsAtTripleZeroSentinel() {
		byte[] head = "xyz".getBytes(StandardCharsets.UTF_8);
		byte[] sentinel = new byte[] { 0, 0, 0 };
		byte[] tail = "end".getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[head.length + sentinel.length + tail.length];
		int pos = 0;
		System.arraycopy(head, 0, data, pos, head.length);
		pos += head.length;
		System.arraycopy(sentinel, 0, data, pos, sentinel.length);
		pos += sentinel.length;
		System.arraycopy(tail, 0, data, pos, tail.length);

		CorruptTripleTerm triple = tripleWithData(data);
		String stringValue = triple.stringValue();
		assertTrue(stringValue.contains("xyz"), "Should include data before sentinel");
		assertFalse(stringValue.contains("end"), "Should not include data after sentinel");
	}

	@Test
	public void returnsNullForComponents() {
		byte[] data = "test".getBytes(StandardCharsets.UTF_8);
		CorruptTripleTerm triple = tripleWithData(data);

		assertNull(triple.getSubject(), "Subject should be null for corrupt triple");
		assertNull(triple.getPredicate(), "Predicate should be null for corrupt triple");
		assertNull(triple.getObject(), "Object should be null for corrupt triple");
	}
}
