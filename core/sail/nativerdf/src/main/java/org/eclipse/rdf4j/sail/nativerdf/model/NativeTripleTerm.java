package org.eclipse.rdf4j.sail.nativerdf.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleTripleTerm;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

public class NativeTripleTerm extends SimpleTripleTerm implements NativeValue {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 5198968663650168820L;

	/*----------*
	 * Variable *
	 *----------*/

	private volatile ValueStoreRevision revision;

	private volatile int internalID;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NativeTripleTerm(ValueStoreRevision revision, Resource subject, IRI predicate, Value object) {
		this(revision, subject, predicate, object, UNKNOWN_ID);
	}

	public NativeTripleTerm(ValueStoreRevision revision, Resource subject, IRI predicate, Value object,
			int internalID) {
		super(subject, predicate, object);
		setInternalID(internalID, revision);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setInternalID(int internalID, ValueStoreRevision revision) {
		this.internalID = internalID;
		this.revision = revision;
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public int getInternalID() {
		return internalID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof NativeTripleTerm otherNativeTriple && internalID != NativeValue.UNKNOWN_ID) {

			if (otherNativeTriple.internalID != NativeValue.UNKNOWN_ID
					&& revision.equals(otherNativeTriple.revision)) {
				// NativeTriple's from the same revision of the same native store,
				// with both ID's set
				return internalID == otherNativeTriple.internalID;
			}
		}

		return super.equals(o);
	}
}
