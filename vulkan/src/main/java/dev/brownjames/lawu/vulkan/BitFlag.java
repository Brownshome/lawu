package dev.brownjames.lawu.vulkan;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

/**
 * A single flag bit in a bitset
 */
public interface BitFlag {
	/**
	 * The bit set by this flag
	 * @return an integer with a single bit set
	 */
	int bit();

	/**
	 * Creates a set of flags from the given elements
	 * @param elements the element, there must be at least one element
	 * @return a set
	 * @param <ELEMENT> the type of the elements
	 */
	@SafeVarargs
	static <ELEMENT extends Enum<ELEMENT> & BitFlag> Set<ELEMENT> flags(ELEMENT... elements) {
		assert elements != null;
		assert Stream.of(elements).allMatch(Objects::nonNull);
		assert elements.length != 0;

		var set = noFlags(elements[0].getDeclaringClass());
		Collections.addAll(set, elements);

		return set;
	}

	/**
	 * Creates a set of flags from a given collection. Where possible this will be an efficient bit-set implementation.
	 * This is only guaranteed if c is not empty.
	 * @param c the collection to convert
	 * @return a set of flags
	 * @param <ELEMENT> the type of element
	 */
	static <ELEMENT extends Enum<ELEMENT> & BitFlag> Set<ELEMENT> flags(Collection<ELEMENT> c) {
		assert c != null;

		if (c.isEmpty()) {
			return new HashSet<>();
		}

		return new BitFlagSet<>(c);
	}

	/**
	 * Creates a set of flags
	 * @param flags the flag bits
	 * @param flagClass the type of flag
	 * @return a set of flags
	 * @param <ELEMENT> the element type
	 */
	static <ELEMENT extends Enum<ELEMENT> & BitFlag> Set<ELEMENT> flags(int flags, Class<ELEMENT> flagClass) {
		return new BitFlagSet<>(flags, flagClass);
	}

	/**
	 * Create an empty flag set with a known type
	 * @param flagClass the type of flag
	 * @return a set of flags
	 * @param <ELEMENT> the element type
	 */
	static <ELEMENT extends Enum<ELEMENT> & BitFlag> Set<ELEMENT> noFlags(Class<ELEMENT> flagClass) {
		return flags(0, flagClass);
	}

	static <ELEMENT extends Enum<ELEMENT> & BitFlag> Set<ELEMENT> allFlags(Class<ELEMENT> c) {
		return flags(c.getEnumConstants());
	}

	/**
	 * Gets an integer containing all set flag bits
	 * @param c the collection to extract the bits
	 * @return an integer
	 * @param <ELEMENT> the element type
	 */
	static <ELEMENT extends Enum<ELEMENT> & BitFlag> int getFlagBits(Collection<ELEMENT> c) {
		if (c.isEmpty()) {
			return 0;
		}

		return new BitFlagSet<>(c).flags();
	}
}

/**
 * A set of flags stored in a bitfield. This set does not support null elements
 *
 * @param <ELEMENT> the type of element in this set
 */
final class BitFlagSet<ELEMENT extends Enum<ELEMENT> & BitFlag> extends AbstractSet<ELEMENT> {
	private int flags;
	private final Class<ELEMENT> type;
	private final ELEMENT[] allValues;

	private static <ELEMENT extends Enum<ELEMENT> & BitFlag> ELEMENT[] createAllValues(ELEMENT[] enumConstants) {
		assert enumConstants != null;

		@SuppressWarnings("unchecked")
		var array = (ELEMENT[]) Array.newInstance(enumConstants.getClass().componentType(), Integer.SIZE);
		for (var e : enumConstants) {
			array[Integer.numberOfTrailingZeros(e.bit())] = e;
		}

		return array;
	}

	BitFlagSet(int flags, Class<ELEMENT> type) {
		assert type != null;

		this.flags = flags;
		this.type = type;
		allValues = createAllValues(type.getEnumConstants());
	}

	BitFlagSet(Collection<ELEMENT> c) {
		assert c != null;
		assert !c.isEmpty();

		if (c instanceof BitFlagSet<ELEMENT> s) {
			this.flags = s.flags;
			this.type = s.type;
			this.allValues = s.allValues;
		} else {
			this.flags = 0;
			this.type = c.iterator().next().getDeclaringClass();
			allValues = createAllValues(type.getEnumConstants());

			assert allValues != null;

			addAll(c);
		}
	}

	int flags() {
		return flags;
	}

	@Override
	public Iterator<ELEMENT> iterator() {
		return new Iterator<>() {
			int flags = BitFlagSet.this.flags;

			@Override
			public boolean hasNext() {
				return flags != 0;
			}

			@Override
			public ELEMENT next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				var result = Integer.SIZE - Integer.numberOfLeadingZeros(flags) - 1;
				assert result >= 0;

				flags &= ~(1 << result);

				return allValues[result];
			}
		};
	}

	@Override
	public int size() {
		return Integer.bitCount(flags);
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof BitFlagSet<?> s && s.flags == flags && s.type.equals(type)) || super.equals(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c instanceof BitFlagSet<?> s && s.type.equals(type)) {
			var modified = (s.flags & flags) != 0;
			flags &= ~s.flags;
			return modified;
		}

		return super.removeAll(c);
	}

	@Override
	public boolean isEmpty() {
		return flags == 0;
	}

	@Override
	public boolean contains(Object o) {
		return (o.getClass().equals(type) || o.getClass().getSuperclass().equals(type))
				&& (flags & ((BitFlag) o).bit()) != 0;
	}

	@Override
	public boolean add(ELEMENT element) {
		if (contains(element)) {
			return false;
		}

		flags |= element.bit();

		return true;
	}

	@Override
	public boolean remove(Object o) {
		if (!contains(o)) {
			return false;
		}

		flags &= ~((BitFlag) o).bit();
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c instanceof BitFlagSet<?> s && s.type.equals(type)) {
			return (flags & s.flags) == s.flags;
		}

		return super.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends ELEMENT> c) {
		if (c instanceof BitFlagSet<?> s && s.type.equals(type)) {
			var modified = !containsAll(c);
			flags |= s.flags;
			return modified;
		}

		return super.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (c instanceof BitFlagSet<?> s && s.type.equals(type)) {
			@SuppressWarnings("SuspiciousMethodCalls")
			var modified = !s.containsAll(this);
			flags &= s.flags;
			return modified;
		}

		return super.retainAll(c);
	}

	@Override
	public void clear() {
		flags = 0;
	}
}
