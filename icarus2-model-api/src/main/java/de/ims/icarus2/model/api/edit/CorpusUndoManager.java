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

import static java.util.Objects.requireNonNull;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.corpus.Corpus;
import de.ims.icarus2.model.api.events.CorpusAdapter;
import de.ims.icarus2.model.api.events.CorpusEvent;

/**
 * @author Markus Gärtner
 *
 */
public class CorpusUndoManager extends UndoManager implements CorpusUndoListener {

	private static final long serialVersionUID = -1207749889681029406L;

	private final Corpus corpus;

	private long savedId = 0L;

	public CorpusUndoManager(Corpus corpus) {
		requireNonNull(corpus);

		this.corpus = corpus;

		corpus.addCorpusListener(new CorpusAdapter(){

			@Override
			public void corpusSaved(CorpusEvent e) {
				markSaved();
			}

		});

		corpus.getEditManager().addCorpusUndoListener(this);
	}

	/**
	 * @return the corpus
	 */
	public Corpus getCorpus() {
		return corpus;
	}

	protected void markSaved() {
		UndoableCorpusEdit edit = editToBeUndone();
		savedId = edit==null ? 0L : edit.getId();
	}

	public boolean isSavedState() {
		UndoableCorpusEdit edit = editToBeUndone();
		long id = edit==null ? 0L : edit.getId();

		return savedId==id;
	}

	/**
	 * @see javax.swing.undo.UndoManager#editToBeUndone()
	 */
	@Override
	protected UndoableCorpusEdit editToBeUndone() {
		return (UndoableCorpusEdit) super.editToBeUndone();
	}

	/**
	 * @see javax.swing.undo.UndoManager#editToBeRedone()
	 */
	@Override
	protected UndoableCorpusEdit editToBeRedone() {
		return (UndoableCorpusEdit) super.editToBeRedone();
	}

	/**
	 * @see javax.swing.undo.UndoManager#addEdit(javax.swing.undo.UndoableEdit)
	 */
	@Override
	public synchronized boolean addEdit(UndoableEdit anEdit) {
		requireNonNull(anEdit);
		if(!(anEdit instanceof UndoableCorpusEdit))
			throw new ModelException(GlobalErrorCode.INVALID_INPUT,
					"Can only handle corpus edits");

		return super.addEdit(anEdit);
	}

	/**
	 * @see javax.swing.undo.AbstractUndoableEdit#replaceEdit(javax.swing.undo.UndoableEdit)
	 */
	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		requireNonNull(anEdit);
		return false;
	}

	/**
	 * @see javax.swing.undo.UndoManager#discardAllEdits()
	 */
	@Override
	public synchronized void discardAllEdits() {
		super.discardAllEdits();
		savedId = 0L;
	}

	/**
	 * @see de.ims.icarus2.model.api.edit.CorpusUndoListener#undoableEditHappened(de.ims.icarus2.model.api.edit.UndoableCorpusEdit)
	 */
	@Override
	public void undoableEditHappened(UndoableCorpusEdit edit) {
		addEdit(edit);
	}

	/**
	 * @see javax.swing.undo.UndoManager#undoableEditHappened(javax.swing.event.UndoableEditEvent)
	 */
	@Override
	public void undoableEditHappened(UndoableEditEvent e) {
		UndoableEdit edit = e.getEdit();
		if(edit instanceof UndoableCorpusEdit) {
			undoableEditHappened((UndoableCorpusEdit) edit);
		} else
			throw new ModelException(GlobalErrorCode.UNSUPPORTED_OPERATION,
					"Use method in "+CorpusUndoListener.class.getSimpleName()+" interface");
	}
}
