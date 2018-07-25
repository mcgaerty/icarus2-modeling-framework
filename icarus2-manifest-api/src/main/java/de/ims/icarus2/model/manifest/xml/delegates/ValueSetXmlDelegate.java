/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2018 Markus Gärtner <markus.gaertner@uni-stuttgart.de>
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
package de.ims.icarus2.model.manifest.xml.delegates;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.ims.icarus2.model.manifest.api.ManifestLocation;
import de.ims.icarus2.model.manifest.api.ValueManifest;
import de.ims.icarus2.model.manifest.api.ValueSet;
import de.ims.icarus2.model.manifest.standard.ValueSetImpl;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.model.manifest.xml.ManifestXmlHandler;
import de.ims.icarus2.model.manifest.xml.ManifestXmlTags;
import de.ims.icarus2.model.manifest.xml.ManifestXmlUtils;
import de.ims.icarus2.util.xml.UnexpectedTagException;
import de.ims.icarus2.util.xml.UnsupportedNestingException;
import de.ims.icarus2.util.xml.XmlSerializer;

/**
 * @author Markus Gärtner
 *
 */
public class ValueSetXmlDelegate extends AbstractXmlDelegate<ValueSet> {

	private ValueManifestXmlDelegate valueManifestXmlDelegate;

	public ValueSetXmlDelegate() {
		//no-op
	}

	public ValueSetXmlDelegate(ValueSet values) {
		setInstance(values);
	}

	public ValueSetXmlDelegate(ValueType valueType) {
		setInstance(new ValueSetImpl(valueType));
	}

	private ValueManifestXmlDelegate getValueManifestXmlDelegate() {
		if(valueManifestXmlDelegate==null) {
			valueManifestXmlDelegate = new ValueManifestXmlDelegate();
		}

		return valueManifestXmlDelegate;
	}

	public ValueSetXmlDelegate reset(ValueType valueType) {
		reset();
		setInstance(new ValueSetImpl(valueType));

		return this;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.delegates.AbstractXmlDelegate#reset()
	 */
	@Override
	public void reset() {
		super.reset();

		if(valueManifestXmlDelegate!=null) {
			valueManifestXmlDelegate.reset();
		}
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.ManifestXmlElement#writeXml(de.ims.icarus2.util.xml.XmlSerializer)
	 */
	@Override
	public void writeXml(XmlSerializer serializer) throws Exception {
		serializer.startElement(ManifestXmlTags.VALUE_SET);

		ValueType type = getInstance().getValueType();

		for(int i=0; i<getInstance().valueCount(); i++) {
			Object value = getInstance().getValueAt(i);

			if(value instanceof ValueManifest) {
				getValueManifestXmlDelegate().reset((ValueManifest) value);
				getValueManifestXmlDelegate().writeXml(serializer);
			} else {
				ManifestXmlUtils.writeValueElement(serializer, ManifestXmlTags.VALUE, value, type);
			}
		}
		serializer.endElement(ManifestXmlTags.VALUE_SET);
	}

	@Override
	public ManifestXmlHandler startElement(ManifestLocation manifestLocation,
			String uri, String localName, String qName, Attributes attributes)
					throws SAXException {
		switch (localName) {
		case ManifestXmlTags.VALUE_SET: {
			// no-op
		} break;

		case ManifestXmlTags.VALUE : {
			if(attributes.getLength()>0) {
				return new ValueManifestXmlDelegate(getInstance().getValueType());
			}
		} break;

		default:
			throw new UnexpectedTagException(qName, true, ManifestXmlTags.VALUE_SET);
		}

		return this;
	}

	@Override
	public ManifestXmlHandler endElement(ManifestLocation manifestLocation,
			String uri, String localName, String qName, String text)
					throws SAXException {
		switch (localName) {
		case ManifestXmlTags.VALUE_SET: {
			return null;
		}

		case ManifestXmlTags.VALUE : {
			Object value = getInstance().getValueType().parse(text, manifestLocation.getClassLoader());

			getInstance().addValue(value);
		} break;

		default:
			throw new UnexpectedTagException(qName, false, ManifestXmlTags.VALUE_SET);
		}

		return this;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.ManifestXmlHandler#endNestedHandler(de.ims.icarus2.model.manifest.api.ManifestLocation, java.lang.String, java.lang.String, java.lang.String, de.ims.icarus2.model.manifest.xml.ManifestXmlHandler)
	 */
	@Override
	public void endNestedHandler(ManifestLocation manifestLocation, String uri,
			String localName, String qName, ManifestXmlHandler handler)
			throws SAXException {
		switch (localName) {

		case ManifestXmlTags.VALUE : {
			Object value = ((ValueManifestXmlDelegate) handler).getInstance();

			getInstance().addValue(value);
		} break;

		default:
			throw new UnsupportedNestingException(qName, ManifestXmlTags.VALUE_SET);
		}
	}
}
