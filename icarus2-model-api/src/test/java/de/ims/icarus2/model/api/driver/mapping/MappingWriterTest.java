/**
 *
 */
package de.ims.icarus2.model.api.driver.mapping;

import de.ims.icarus2.model.api.io.SynchronizedAccessorTest;

/**
 * @author Markus Gärtner
 *
 */
public interface MappingWriterTest extends SynchronizedAccessorTest<WritableMapping, MappingWriter> {

	/**
	 * @see de.ims.icarus2.model.api.io.SynchronizedAccessorTest#createAccessor(java.lang.Object)
	 */
	@Override
	default MappingWriter createAccessor(WritableMapping source) {
		return source.newWriter();
	}
}