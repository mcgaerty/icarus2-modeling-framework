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
package de.ims.icarus2.model.manifest.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.ims.icarus2.model.manifest.api.ManifestLocation;

/**
 * @author Markus Gärtner
 *
 */
public interface ManifestXmlHandler {

	ManifestXmlHandler startElement(ManifestLocation manifestLocation, String uri, String localName, String qName,
			Attributes attributes) throws SAXException;

	ManifestXmlHandler endElement(ManifestLocation manifestLocation, String uri, String localName, String qName, String text)
			throws SAXException;

	void endNestedHandler(ManifestLocation manifestLocation, String uri, String localName, String qName,
			ManifestXmlHandler handler) throws SAXException;
}
