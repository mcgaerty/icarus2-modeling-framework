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

import java.net.URI;
import java.net.URISyntaxException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.ims.icarus2.model.manifest.api.Documentation;
import de.ims.icarus2.model.manifest.api.Documentation.Resource;
import de.ims.icarus2.model.manifest.api.ManifestLocation;
import de.ims.icarus2.model.manifest.standard.DocumentationImpl.ResourceImpl;
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
public class DocumentationXmlDelegate extends AbstractXmlDelegate<Documentation> {

	private Resource resource;

	public DocumentationXmlDelegate() {
		//no-op
	}

	public DocumentationXmlDelegate(Documentation documentation) {
		setInstance(documentation);
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.delegates.AbstractXmlDelegate#reset()
	 */
	@Override
	public void reset() {
		super.reset();

		resource = null;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.ManifestXmlHandler#startElement(de.ims.icarus2.model.manifest.api.ManifestLocation, java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public ManifestXmlHandler startElement(ManifestLocation manifestLocation,
			String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
		switch (localName) {
		case ManifestXmlTags.DOCUMENTATION: {
			ManifestXmlUtils.readIdentityAttributes(attributes, getInstance());
		} break;

		case ManifestXmlTags.RESOURCE: {
			resource = new ResourceImpl();
			ManifestXmlUtils.readIdentityAttributes(attributes, resource);
		} break;

		case ManifestXmlTags.CONTENT: {
			// no-op
		} break;

		default:
			throw new UnexpectedTagException(qName, true, ManifestXmlTags.DOCUMENTATION);
		}

		return this;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.ManifestXmlHandler#endElement(de.ims.icarus2.model.manifest.api.ManifestLocation, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public ManifestXmlHandler endElement(ManifestLocation manifestLocation,
			String uri, String localName, String qName, String text)
			throws SAXException {
		switch (localName) {
		case ManifestXmlTags.DOCUMENTATION: {
			return null;
		}

		case ManifestXmlTags.CONTENT: {
			getInstance().setContent(text);
		} break;

		case ManifestXmlTags.RESOURCE: {

			if(text!=null) {
				try {
					resource.setUri(new URI(text));
				} catch (URISyntaxException e) {
					throw new SAXException("Invalid resoucre uri", e); //$NON-NLS-1$
				}
			}

			getInstance().addResource(resource);
			resource = null;
		} break;

		default:
			throw new UnexpectedTagException(qName, false, ManifestXmlTags.DOCUMENTATION);
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
		throw new UnsupportedNestingException(qName, ManifestXmlTags.DOCUMENTATION);
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.ManifestXmlDelegate#writeXml(de.ims.icarus2.util.xml.XmlSerializer)
	 */
	@Override
	public void writeXml(XmlSerializer serializer) throws Exception {
		Documentation documentation = getInstance();

		serializer.startElement(ManifestXmlTags.DOCUMENTATION);
		ManifestXmlUtils.writeIdentityAttributes(serializer, documentation);

		if(documentation.getContent()!=null) {
			serializer.startElement(ManifestXmlTags.CONTENT);
			serializer.writeCData(documentation.getContent());
			serializer.endElement(ManifestXmlTags.CONTENT);
		}

		for(Resource resource : documentation.getResources()) {
			if(resource.getUri()==null)
				throw new IllegalStateException("Resource is missing url"); //$NON-NLS-1$

			serializer.startElement(ManifestXmlTags.RESOURCE);
			ManifestXmlUtils.writeIdentityAttributes(serializer, resource);
			serializer.writeTextOrCData(resource.getUri().toString());
			serializer.endElement(ManifestXmlTags.RESOURCE);
		}

		serializer.endElement(ManifestXmlTags.DOCUMENTATION);
	}
}