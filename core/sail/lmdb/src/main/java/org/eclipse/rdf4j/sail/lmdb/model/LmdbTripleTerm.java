package org.eclipse.rdf4j.sail.lmdb.model;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractTripleTerm;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbTripleTerm extends AbstractTripleTerm implements LmdbValue {

	/*-----------*
	 * Constants *
	 *-----------*/
	@Serial
	private static final long serialVersionUID = 5198968663650168820L;

	/*----------*
	 * Variable *
	 *----------*/

	private Resource subject;
	private IRI predicate;
	private Value object;

	private ValueStoreRevision revision;
	private long internalID;
	private boolean initialized = false;

	/*--------------*
	 * Constructors *
	 *--------------*/
	public LmdbTripleTerm(ValueStoreRevision revision, long internalID) {
		super();
		setInternalID(internalID, revision);
	}

	public LmdbTripleTerm(ValueStoreRevision revision, Resource subject, IRI predicate, Value object) {
		this(revision, subject, predicate, object, UNKNOWN_ID);
	}

	public LmdbTripleTerm(ValueStoreRevision revision, Resource subject, IRI predicate, Value object, long internalID) {
		Objects.requireNonNull(subject, "subject cannot be null");
		Objects.requireNonNull(predicate, "predicate cannot be null");
		Objects.requireNonNull(object, "object cannot be null");
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setInternalID(long internalID, ValueStoreRevision revision) {
		this.internalID = internalID;
		this.revision = revision;
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public void setFromInitializedValue(LmdbValue initializedValue) {
		if (initializedValue instanceof LmdbTripleTerm lmdbTripleTerm) {
			this.subject = lmdbTripleTerm.subject;
			this.predicate = lmdbTripleTerm.predicate;
			this.object = lmdbTripleTerm.object;
		} else {
			throw new IllegalArgumentException("Initialized value is not of type LmdbTripleTerm");
		}
	}

	@Override
	public long getInternalID() {
		return internalID;
	}

	@Override
	public Resource getSubject() {
		init();
		return subject;
	}

	@Override
	public IRI getPredicate() {
		init();
		return predicate;
	}

	@Override
	public Value getObject() {
		init();
		return object;
	}

	public void setSubject(Resource subject) {
		this.subject = subject;
	}

	public void setPredicate(IRI predicate) {
		this.predicate = predicate;
	}

	public void setObject(Value object) {
		this.object = object;
	}

	public void init() {
		if (!initialized) {
			synchronized (this) {
				if (!initialized) {
					boolean resolved = revision.resolveValue(internalID, this);
					initialized = resolved;
					assert resolved;
				}
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof LmdbTripleTerm otherLmdbTripleTerm && internalID != UNKNOWN_ID) {
			if (otherLmdbTripleTerm.internalID != UNKNOWN_ID
					&& revision.equals(otherLmdbTripleTerm.revision)) {
				return internalID == otherLmdbTripleTerm.internalID;
			}
		}

		init();
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (internalID != UNKNOWN_ID) {
			int cachedHash = revision.getStoredHash(internalID);
			if (cachedHash != 0) {
				return cachedHash;
			}
		}

		init();
		int hash = super.hashCode();
		if (internalID != UNKNOWN_ID) {
			revision.storeHash(internalID, hash);
		}
		return hash;
	}

	@Override
	public String toString() {
		init();
		return super.toString();
	}

	@Serial
	protected Object writeReplace() throws ObjectStreamException {
		init();
		return this;
	}
}
