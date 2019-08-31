/**
 *
 */
package de.ims.icarus2.model.api.edit.io;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import de.ims.icarus2.model.api.members.CorpusMember;
import de.ims.icarus2.model.api.members.MemberType;
import de.ims.icarus2.model.api.raster.Position;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.util.collections.seq.DataSequence;

/**
 * @author Markus Gärtner
 *
 */
public class ChangeBuffer implements ChangeWriter, ChangeReader {

	private final Deque<Object> buffer = new ArrayDeque<>(200);

	@SuppressWarnings("unchecked")
	private <T> T pop() {
		return (T) buffer.removeFirst();
	}

	private void push(Object obj) {
		buffer.addLast(obj);
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readMember(de.ims.icarus2.model.api.members.MemberType)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <M extends CorpusMember> M readMember(MemberType type) throws IOException {
		return (M) pop();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readLong()
	 */
	@Override
	public long readLong() throws IOException {
		return ((Long)pop()).longValue();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readFloat()
	 */
	@Override
	public float readFloat() throws IOException {
		return ((Float)pop()).floatValue();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readDouble()
	 */
	@Override
	public double readDouble() throws IOException {
		return ((Double)pop()).doubleValue();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readInt()
	 */
	@Override
	public int readInt() throws IOException {
		return ((Integer)pop()).intValue();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readBoolean()
	 */
	@Override
	public boolean readBoolean() throws IOException {
		return ((Boolean)pop()).booleanValue();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readString()
	 */
	@Override
	public String readString() throws IOException {
		return (String)pop();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readValue()
	 */
	@Override
	public Object readValue() throws IOException {
		return pop();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readPosition()
	 */
	@Override
	public Position readPosition() throws IOException {
		return (Position) pop();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeReader#readSequence(de.ims.icarus2.model.api.members.MemberType)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <M extends CorpusMember> DataSequence<M> readSequence(MemberType type) throws IOException {
		return (DataSequence<M>) pop();
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeMember(de.ims.icarus2.model.api.members.CorpusMember)
	 */
	@Override
	public void writeMember(CorpusMember member) throws IOException {
		push(member);
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeLong(long)
	 */
	@Override
	public void writeLong(long value) throws IOException {
		push(Long.valueOf(value));
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeFloat(float)
	 */
	@Override
	public void writeFloat(float value) throws IOException {
		push(Float.valueOf(value));
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeDouble(double)
	 */
	@Override
	public void writeDouble(double value) throws IOException {
		push(Double.valueOf(value));
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeInt(int)
	 */
	@Override
	public void writeInt(int value) throws IOException {
		push(Integer.valueOf(value));
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeBoolean(boolean)
	 */
	@Override
	public void writeBoolean(boolean b) throws IOException {
		push(Boolean.valueOf(b));
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeString(java.lang.String)
	 */
	@Override
	public void writeString(String s) throws IOException {
		push(s);
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeValue(de.ims.icarus2.model.manifest.types.ValueType, java.lang.Object)
	 */
	@Override
	public void writeValue(ValueType type, Object value) throws IOException {
		push(value);
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writePosition(de.ims.icarus2.model.api.raster.Position)
	 */
	@Override
	public void writePosition(Position position) throws IOException {
		push(position);
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.io.ChangeWriter#writeSequence(de.ims.icarus2.util.collections.seq.DataSequence)
	 */
	@Override
	public void writeSequence(DataSequence<? extends CorpusMember> list) throws IOException {
		push(list);
	}
}
