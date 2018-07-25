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

import de.ims.icarus2.model.manifest.api.ContainerManifest;
import de.ims.icarus2.model.manifest.api.FragmentLayerManifest;
import de.ims.icarus2.model.manifest.api.LayerGroupManifest;
import de.ims.icarus2.model.manifest.api.ManifestLocation;
import de.ims.icarus2.model.manifest.standard.FragmentLayerManifestImpl;
import de.ims.icarus2.model.manifest.xml.ManifestXmlAttributes;
import de.ims.icarus2.model.manifest.xml.ManifestXmlHandler;
import de.ims.icarus2.model.manifest.xml.ManifestXmlTags;
import de.ims.icarus2.model.manifest.xml.ManifestXmlUtils;
import de.ims.icarus2.util.xml.XmlSerializer;

/**
 * @author Markus Gärtner
 *
 */
public class FragmentLayerManifestXmlDelegate extends AbstractLayerManifestXmlDelegate<FragmentLayerManifest> {

	private RasterizerManifestXmLDelegate rasterizerManifestXmLDelegate;
	private ContainerManifestXmlDelegate containerManifestXmlDelegate;

	public FragmentLayerManifestXmlDelegate() {
		// no-op
	}

	public FragmentLayerManifestXmlDelegate(FragmentLayerManifest manifest) {
		setInstance(manifest);
	}

	public FragmentLayerManifestXmlDelegate(LayerGroupManifest groupManifest) {
		setInstance(new FragmentLayerManifestImpl(groupManifest));
	}

	private RasterizerManifestXmLDelegate getRasterizerManifestXmLDelegate() {
		if(rasterizerManifestXmLDelegate==null) {
			rasterizerManifestXmLDelegate = new RasterizerManifestXmLDelegate();
		}

		return rasterizerManifestXmLDelegate;
	}

	private ContainerManifestXmlDelegate getContainerManifestXmlDelegate() {
		if(containerManifestXmlDelegate==null) {
			containerManifestXmlDelegate = new ContainerManifestXmlDelegate();
		}

		return containerManifestXmlDelegate;
	}

	public FragmentLayerManifestXmlDelegate reset(LayerGroupManifest groupManifest) {
		reset();
		setInstance(new FragmentLayerManifestImpl(groupManifest));

		return this;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.xml.delegates.AbstractXmlDelegate#reset()
	 */
	@Override
	public void reset() {
		super.reset();

		if(rasterizerManifestXmLDelegate!=null) {
			rasterizerManifestXmLDelegate.reset();
		}

		if(containerManifestXmlDelegate!=null) {
			containerManifestXmlDelegate.reset();
		}
	}

	/**
	 * @see de.ims.icarus2.model.manifest.standard.AbstractMemberManifest#readAttributes(org.xml.sax.Attributes)
	 */
	@Override
	protected void readAttributes(Attributes attributes) {
		super.readAttributes(attributes);

		String annotationKey = ManifestXmlUtils.normalize(attributes, ManifestXmlAttributes.ANNOTATION_KEY);
		if(annotationKey!=null) {
			getInstance().setAnnotationKey(annotationKey);
		}
	}

	/**
	 * @see de.ims.icarus2.model.manifest.standard.AbstractLayerManifest#writeAttributes(de.ims.icarus2.util.xml.XmlSerializer)
	 */
	@Override
	protected void writeAttributes(XmlSerializer serializer) throws Exception {
		super.writeAttributes(serializer);

		FragmentLayerManifest manifest = getInstance();

		if(manifest.isLocalAnnotationKey()) {
			serializer.writeAttribute(ManifestXmlAttributes.ANNOTATION_KEY, manifest.getAnnotationKey());
		}
	}

	/**
	 * @see de.ims.icarus2.model.manifest.standard.AbstractLayerManifest#writeElements(de.ims.icarus2.util.xml.XmlSerializer)
	 */
	@Override
	protected void writeElements(XmlSerializer serializer) throws Exception {
		super.writeElements(serializer);

		FragmentLayerManifest manifest = getInstance();

		if(manifest.isLocalBoundaryLayerManifest()) {
			ManifestXmlUtils.writeTargetLayerManifestElement(serializer, ManifestXmlTags.BOUNDARY_LAYER, manifest.getBoundaryLayerManifest());
		}

		if(manifest.isLocalFoundationLayerManifest()) {
			ManifestXmlUtils.writeTargetLayerManifestElement(serializer, ManifestXmlTags.FOUNDATION_LAYER, manifest.getFoundationLayerManifest());
		}

		if(manifest.hasLocalContainers()) {
			for(ContainerManifest containerManifest : manifest.getContainerManifests()) {
				getContainerManifestXmlDelegate().reset(containerManifest).writeXml(serializer);
			}
		}

		if(manifest.isLocalValueLayerManifest()) {
			ManifestXmlUtils.writeTargetLayerManifestElement(serializer, ManifestXmlTags.VALUE_LAYER, manifest.getValueLayerManifest());
		}

		if(manifest.isLocalRasterizerManifest()) {
			getRasterizerManifestXmLDelegate().reset(manifest.getRasterizerManifest()).writeXml(serializer);
		}
	}

	@Override
	public ManifestXmlHandler startElement(ManifestLocation manifestLocation,
			String uri, String localName, String qName, Attributes attributes)
					throws SAXException {
		switch (localName) {
		case ManifestXmlTags.FRAGMENT_LAYER: {
			readAttributes(attributes);
		} break;

		case ManifestXmlTags.BOUNDARY_LAYER: {
			String boundaryLayerId = ManifestXmlUtils.normalize(attributes, ManifestXmlAttributes.LAYER_ID);
			getInstance().setBoundaryLayerId(boundaryLayerId);
		} break;

		case ManifestXmlTags.FOUNDATION_LAYER: {
			String foundationLayerId = ManifestXmlUtils.normalize(attributes, ManifestXmlAttributes.LAYER_ID);
			getInstance().setFoundationLayerId(foundationLayerId);
		} break;

		case ManifestXmlTags.CONTAINER: {
			return getContainerManifestXmlDelegate().reset(getInstance());
		}

		case ManifestXmlTags.VALUE_LAYER: {
			String valueLayerId = ManifestXmlUtils.normalize(attributes, ManifestXmlAttributes.LAYER_ID);
			getInstance().setValueLayerId(valueLayerId);
		} break;

		case ManifestXmlTags.RASTERIZER: {
			return getRasterizerManifestXmLDelegate().reset(getInstance());
		}

		default:
			return super.startElement(manifestLocation, uri, localName, qName, attributes);
		}

		return this;
	}

	@Override
	public ManifestXmlHandler endElement(ManifestLocation manifestLocation,
			String uri, String localName, String qName, String text)
					throws SAXException {
		switch (localName) {
		case ManifestXmlTags.FRAGMENT_LAYER: {
			return null;
		}

		case ManifestXmlTags.BOUNDARY_LAYER: {
			// no-op
		} break;

		case ManifestXmlTags.FOUNDATION_LAYER: {
			// no-op
		} break;

		case ManifestXmlTags.VALUE_LAYER: {
			// no-op
		} break;

		default:
			return super.endElement(manifestLocation, uri, localName, qName, text);
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

		case ManifestXmlTags.CONTAINER: {
			getInstance().addContainerManifest(((ContainerManifestXmlDelegate) handler).getInstance(), -1);
		} break;

		case ManifestXmlTags.RASTERIZER: {
			getInstance().setRasterizerManifest(((RasterizerManifestXmLDelegate) handler).getInstance());
		} break;

		default:
			super.endNestedHandler(manifestLocation, uri, localName, qName, handler);
			break;
		}
	}

	/**
	 * @see de.ims.icarus2.model.manifest.standard.AbstractManifest#xmlTag()
	 */
	@Override
	protected String xmlTag() {
		return ManifestXmlTags.FRAGMENT_LAYER;
	}
}