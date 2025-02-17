/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2025 Markus Gärtner <markus.gaertner@ims.uni-stuttgart.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ims.icarus2.model.api.edit;

import static de.ims.icarus2.util.lang.Primitives._boolean;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.ims.icarus2.model.api.corpus.Corpus;
import de.ims.icarus2.model.api.corpus.GenerationControl;
import de.ims.icarus2.test.ApiGuardedTest;
import de.ims.icarus2.test.TestSettings;

/**
 * @author Markus Gärtner
 *
 */
class CorpusUndoManagerTest implements ApiGuardedTest<CorpusUndoManager> {

	@Override
	public Class<?> getTestTargetClass() {
		return CorpusUndoManager.class;
	}

	@Override
	public CorpusUndoManager createTestInstance(TestSettings settings) {
		CorpusEditManager editManager = mock(CorpusEditManager.class);
		Corpus corpus = mock(Corpus.class);
		when(corpus.getEditManager()).thenReturn(editManager);
		return settings.process(new CorpusUndoManager(corpus));
	}

	private static UndoableCorpusEdit mockEdit(boolean undoable, boolean redoable) {
		UndoableCorpusEdit edit = mock(UndoableCorpusEdit.class);
		when(_boolean(edit.canRedo())).thenReturn(_boolean(redoable));
		when(_boolean(edit.canUndo())).thenReturn(_boolean(undoable));
		when(_boolean(edit.isSignificant())).thenReturn(Boolean.TRUE);
		return edit;
	}

	@Nested
	class WithCorpus {

		Corpus corpus;
		CorpusUndoManager manager;
		CorpusEditManager editManager;
		GenerationControl generations;

		@BeforeEach
		void setUp() {
			generations = mock(GenerationControl.class);
			Lock lock = mock(Lock.class);
			editManager = mock(CorpusEditManager.class);

			corpus = mock(Corpus.class);
			when(corpus.getGenerationControl()).thenReturn(generations);
			when(corpus.getLock()).thenReturn(lock);
			when(corpus.getEditManager()).thenReturn(editManager);

			manager = new CorpusUndoManager(corpus);
		}

		@AfterEach
		void tearDown() throws Exception {
			generations.close();
			generations = null;
			editManager = null;
			corpus.close();
			corpus = null;
			manager = null;
		}

		@Test
		void constructor() {
			// constructor called by setUp() method
			assertSame(corpus, manager.getCorpus());
			assertTrue(manager.isSavedState());
			assertTrue(manager.isInProgress());
		}

		@Test
		void empty() {
			assertFalse(manager.canRedo());
			assertFalse(manager.canUndo());
			assertFalse(manager.canUndoOrRedo());
		}

		@Test
		void singleAdded() {
			UndoableCorpusEdit edit = mockEdit(true, false);
			assertTrue(manager.addEdit(edit));

			assertFalse(manager.canRedo());
			assertTrue(manager.canUndo());
			assertTrue(manager.canUndoOrRedo());

			verify(edit, atLeastOnce()).canUndo();
			verify(edit, never()).canRedo();
		}

		//TODO add tests for verifying real undo/redo sequences and generation control
	}
}
