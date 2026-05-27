/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

/**
 * CorruptTriple is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled
 *
 * @see NativeStore#SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES .
 *
 * @author Sava B. Savov
 */
public class CorruptTripleTerm extends CorruptValue implements TripleTerm {

	private static final long serialVersionUID = -2510885288827542624L;

	public CorruptTripleTerm(ValueStoreRevision revision, int internalID, byte[] data) {
		super(revision, internalID, data);
	}

	@Override
	public String stringValue() {
		return "CorruptTripleTerm_with_ID_" + getInternalID() + ": " + getTripleString();
	}

	private String getTripleString() {
		byte[] data = getData();
		try {
			if (data != null && data.length > 0) {
				// check if all bytes are zero
				boolean allZero = true;
				for (byte b : data) {
					if (b != 0) {
						allZero = false;
						break;
					}
				}

				if (allZero) {
					return "All " + data.length + " data bytes are 0x00";
				}

				String prefix = this.getClass().getSimpleName() + " with ID " + getInternalID()
						+ " with possible data: ";

				data = truncateData(data);

				// 1) Try full UTF-8 decode of the slice
				try {
					String utf8 = new String(data, StandardCharsets.UTF_8);
					if (utf8.indexOf('\uFFFD') < 0) {
						return prefix + utf8;
					}
				} catch (Throwable ignored) {
				}

				// 2) Longest clean UTF-8 substring
				String recoveredUtf8 = null;
				int bestLen = 0;
				for (int start = 0; start < data.length; start++) {
					for (int end = data.length; end > start; end--) {
						int len = end - start;
						if (len <= bestLen) {
							break;
						}
						try {
							String s = new String(data, start, len, StandardCharsets.UTF_8);
							if (s.indexOf('\uFFFD') < 0) {
								recoveredUtf8 = s;
								bestLen = len;
								break;
							}
						} catch (Throwable ignored) {
						}
					}
				}
				if (recoveredUtf8 != null && !recoveredUtf8.isEmpty()) {
					return prefix + recoveredUtf8;
				}

				// 3) Longest contiguous printable ASCII run in slice
				int bestAsciiStart = -1;
				int bestAsciiLen = 0;
				int i = 0;
				while (i < data.length) {
					if (data[i] >= 0x20 && data[i] <= 0x7E) {
						int runStart = i;
						while (i < data.length && data[i] >= 0x20 && data[i] <= 0x7E) {
							i++;
						}
						int runLen = i - runStart;
						if (runLen > bestAsciiLen) {
							bestAsciiLen = runLen;
							bestAsciiStart = runStart;
						}
					} else {
						i++;
					}
				}
				if (bestAsciiLen > 0) {
					String ascii = new String(data, bestAsciiStart, bestAsciiLen, StandardCharsets.US_ASCII);
					return prefix + ascii;
				}

				// 4) Fallback: hex encode only up to sentinel data.length
				return prefix + Hex.encodeHexString(Arrays.copyOfRange(data, 0, data.length));
			}
		} catch (Throwable ignored) {
		}
		return this.getClass().getSimpleName() + " with ID " + getInternalID();
	}

	@Override
	public Resource getSubject() {
		// Return a corrupt placeholder or null
		return null;
	}

	@Override
	public IRI getPredicate() {
		// Return a corrupt placeholder or null
		return null;
	}

	@Override
	public Value getObject() {
		// Return a corrupt placeholder or null
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof CorruptTripleTerm otherCorruptTriple && getInternalID() != NativeValue.UNKNOWN_ID) {
			if (otherCorruptTriple.getInternalID() != NativeValue.UNKNOWN_ID
					&& getValueStoreRevision().equals(otherCorruptTriple.getValueStoreRevision())) {
				// CorruptTriple is from the same revision of the same native store with both IDs set
				return getInternalID() == otherCorruptTriple.getInternalID();
			}
		}

		return super.equals(o);
	}
}
