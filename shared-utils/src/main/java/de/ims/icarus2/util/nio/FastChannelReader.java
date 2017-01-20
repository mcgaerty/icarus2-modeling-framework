/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015-2016 Markus Gärtner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 */
package de.ims.icarus2.util.nio;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * This is kind of a clone of the internal {@code sun.nio.cs.StreamDecoder} provided by the JRE via
 * {@link Channels#newReader(java.nio.channels.ReadableByteChannel, java.nio.charset.CharsetDecoder, int)}
 * . The main difference is that this implementation is designed for
 * single-thread use and therefore does not use any synchronization. In addition
 * it will make use of a {@link ByteBuffer#allocateDirect(int) directly
 * allocated} {@link ByteBuffer} to improve performance.
 *
 * @author Markus Gärtner
 *
 */
public class FastChannelReader extends Reader {

	private static final int MIN_BYTE_BUFFER_SIZE = 32;
	private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;

	private volatile boolean isOpen = true;

	private void ensureOpen() throws IOException {
		if (!isOpen)
			throw new IOException("Stream closed");
	}

	// In order to handle surrogates properly we must never try to produce
	// fewer than two characters at a time. If we're only asked to return one
	// character then the other is saved here to be returned later.
	//
	private boolean haveLeftoverChar = false;
	private char leftoverChar;

	private final Charset cs;
	private final CharsetDecoder decoder;
	private final ByteBuffer bb;

	private final ReadableByteChannel ch;

	public FastChannelReader(ReadableByteChannel ch, Charset cs, int mbc) {
		this(ch, cs.newDecoder(), mbc);
	}

	public FastChannelReader(ReadableByteChannel ch, CharsetDecoder dec, int mbc) {
		requireNonNull(ch);
		requireNonNull(dec);

		this.ch = ch;
		this.decoder = dec;
		this.cs = dec.charset();
		this.bb = ByteBuffer.allocateDirect(mbc < 0 ? DEFAULT_BYTE_BUFFER_SIZE
				: (mbc < MIN_BYTE_BUFFER_SIZE ? MIN_BYTE_BUFFER_SIZE : mbc));
		bb.flip();
	}

	public Charset getCharset() {
		return cs;
	}

	public ReadableByteChannel getChannel() {
		return ch;
	}

	@Override
	public int read() throws IOException {
		return read0();
	}

	private final char[] cb = new char[2];

	@SuppressWarnings("fallthrough")
	private int read0() throws IOException {
		// Return the leftover char, if there is one
		if (haveLeftoverChar) {
			haveLeftoverChar = false;
			return leftoverChar;
		}

		// Convert more bytes
		int n = read(cb, 0, 2);
		switch (n) {
		case -1:
			return -1;
		case 2:
			leftoverChar = cb[1];
			haveLeftoverChar = true;
			// FALL THROUGH
		case 1:
			return cb[0];
		default:
			assert false : n;
			return -1;
		}
	}

	@Override
	public int read(char cbuf[], int offset, int length) throws IOException {
		int off = offset;
		int len = length;

		ensureOpen();
		if ((off < 0) || (off > cbuf.length) || (len < 0)
				|| ((off + len) > cbuf.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		}
		if (len == 0)
			return 0;

		int n = 0;

		if (haveLeftoverChar) {
			// Copy the leftover char into the buffer
			cbuf[off] = leftoverChar;
			off++;
			len--;
			haveLeftoverChar = false;
			n = 1;
			if ((len == 0) || !implReady())
				// Return now if this is all we can produce w/o blocking
				return n;
		}

		if (len == 1) {
			// Treat single-character array reads just like read()
			int c = read0();
			if (c == -1)
				return (n == 0) ? -1 : n;
			cbuf[off] = (char) c;
			return n + 1;
		}

		return n + implRead(cbuf, off, off + len);
	}

	@Override
	public boolean ready() throws IOException {
		ensureOpen();
		return haveLeftoverChar || implReady();
	}

	@Override
	public void close() throws IOException {
		if (!isOpen)
			return;
		implClose();
		isOpen = false;
	}

	private int readBytes() throws IOException {
		bb.compact();
		try {
			// Read from the channel
			int n = ch.read(bb);
			if (n < 0)
				return n;
		} finally {
			// Flip even when an IOException is thrown,
			// otherwise the stream will stutter
			bb.flip();
		}

		return bb.remaining();
	}

	int implRead(char[] cbuf, int off, int end) throws IOException {

		// In order to handle surrogate pairs, this method requires that
		// the invoker attempt to read at least two characters. Saving the
		// extra character, if any, at a higher level is easier than trying
		// to deal with it here.
		assert (end - off > 1);

		CharBuffer cb = CharBuffer.wrap(cbuf, off, end - off);
		if (cb.position() != 0)
			// Ensure that cb[0] == cbuf[off]
			cb = cb.slice();

		boolean eof = false;
		for (;;) {
			CoderResult cr = decoder.decode(bb, cb, eof);
			if (cr.isUnderflow()) {
				if (eof)
					break;
				if (!cb.hasRemaining())
					break;
				if ((cb.position() > 0) && !inReady())
					break; // Block at most once
				int n = readBytes();
				if (n < 0) {
					eof = true;
					if ((cb.position() == 0) && (!bb.hasRemaining()))
						break;
					decoder.reset();
				}
				continue;
			}
			if (cr.isOverflow()) {
				assert cb.position() > 0;
				break;
			}
			cr.throwException();
		}

		if (eof) {
			// ## Need to flush decoder
			decoder.reset();
		}

		if (cb.position() == 0) {
			if (eof)
				return -1;
			assert false;
		}
		return cb.position();
	}

	private boolean inReady() {
		return true; //TODO verify that we do not need some additional checking here
//		try {
//			return (((in != null) && (in.available() > 0)) || (ch instanceof FileChannel)); // ##
//																							// RBC.available()?
//		} catch (IOException x) {
//			return false;
//		}
	}

	boolean implReady() {
		return bb.hasRemaining() || inReady();
	}

	void implClose() throws IOException {
		ch.close();
	}

	@Override
	public void reset() {
		haveLeftoverChar = false;
		decoder.reset();
		bb.clear();
	}
}
