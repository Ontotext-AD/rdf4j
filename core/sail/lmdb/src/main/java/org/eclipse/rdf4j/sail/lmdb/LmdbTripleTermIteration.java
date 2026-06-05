package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.sail.SailException;

public class LmdbTripleTermIteration implements CloseableIteration<TripleTerm> {

	private final RecordIterator records;
	private final ValueStore valueStore;
	private final long tripleTermID;
	private TripleTerm next;

	private final List<TripleTerm> pending = new ArrayList<>();

	LmdbTripleTermIteration(RecordIterator records, ValueStore valueStore, long tripleTermID) {
		this.records = records;
		this.valueStore = valueStore;
		this.tripleTermID = tripleTermID;
	}

	@Override
	public boolean hasNext() throws SailException {
		if (next != null) {
			return true;
		}
		if (!pending.isEmpty()) {
			next = pending.removeFirst();
			return true;
		}
		try {
			long[] quad = records.next();
			if (quad != null) {
				next = (TripleTerm) valueStore.getValue(tripleTermID);
				collectNested(next);
				return true;
			}
			return false;
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public TripleTerm next() throws SailException {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		TripleTerm result = next;
		next = null;
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws SailException {
		records.close();
	}

	private void collectNested(TripleTerm triple) {
		if (triple.getObject().isTripleTerm()) {
			TripleTerm nested = (TripleTerm) triple.getObject();
			pending.add(nested);
			collectNested(nested); // recurse for deeper nesting
		}
	}
}
