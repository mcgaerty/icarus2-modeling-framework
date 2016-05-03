/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015 Markus Gärtner
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

 * $Revision: 445 $
 * $Date: 2016-01-11 17:33:05 +0100 (Mo, 11 Jan 2016) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/standard/manifest/LocationManifestImpl.java $
 *
 * $LastChangedDate: 2016-01-11 17:33:05 +0100 (Mo, 11 Jan 2016) $
 * $LastChangedRevision: 445 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.standard.manifest;

import static de.ims.icarus2.model.util.Conditions.checkArgument;
import static de.ims.icarus2.model.util.Conditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.ims.icarus2.model.api.manifest.ContextManifest;
import de.ims.icarus2.model.api.manifest.LocationManifest;
import de.ims.icarus2.model.api.manifest.ManifestErrorCode;
import de.ims.icarus2.model.api.manifest.ManifestException;
import de.ims.icarus2.model.api.manifest.ManifestLocation;
import de.ims.icarus2.model.api.manifest.ManifestRegistry;
import de.ims.icarus2.model.api.manifest.ManifestType;
import de.ims.icarus2.model.api.manifest.PathResolverManifest;
import de.ims.icarus2.util.classes.ClassUtils;

/**
 * @author Markus Gärtner
 * @version $Id: LocationManifestImpl.java 445 2016-01-11 16:33:05Z mcgaerty $
 *
 */
public class LocationManifestImpl extends AbstractManifest<LocationManifest> implements LocationManifest {
	private String rootPath;
	private PathType rootPathType;
	private PathResolverManifest pathResolverManifest;

	private final List<PathEntry> pathEntries = new ArrayList<>();

	public LocationManifestImpl(ManifestLocation manifestLocation,
			ManifestRegistry registry) {
		super(manifestLocation, registry, false);
	}

	public LocationManifestImpl(ManifestLocation manifestLocation,
			ManifestRegistry registry, String path) {
		super(manifestLocation, registry, false);
		setRootPath(path);
	}

	public LocationManifestImpl(ContextManifest contextManifest) {
		this(contextManifest.getManifestLocation(), contextManifest.getRegistry());
	}

	@Override
	protected boolean isTopLevel() {
		return false;
	}

	@Override
	public int hashCode() {
		int h = super.hashCode();

		if(rootPath!=null) {
			h *= rootPath.hashCode();
		}
		if(rootPathType!=null) {
			h *= rootPathType.hashCode();
		}

		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		} else if(obj instanceof LocationManifest) {
			LocationManifest other = (LocationManifest) obj;
			return ClassUtils.equals(getRootPath(), other.getRootPath())
					&& ClassUtils.equals(getRootPathType(), other.getRootPathType())
					&& super.equals(obj);
		}
		return false;
	}

	/**
	 * @see de.ims.icarus2.model.standard.manifest.AbstractManifest#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return rootPath==null && pathResolverManifest==null && pathEntries.isEmpty();
	}

	@Override
	public ManifestType getManifestType() {
		return ManifestType.LOCATION_MANIFEST;
	}

	/**
	 * @see de.ims.icarus2.model.api.manifest.LocationManifest#getRootPath()
	 */
	@Override
	public String getRootPath() {
		return rootPath;
	}

	/**
	 * @see de.ims.icarus2.model.api.manifest.LocationManifest#getPathResolverManifest()
	 */
	@Override
	public PathResolverManifest getPathResolverManifest() {
		return pathResolverManifest;
	}

	/**
	 * @param rootPath the rootPath to set
	 */
	@Override
	public void setRootPath(String rootPath) {
		checkNotLocked();

		setRootPath0(rootPath);
	}

	protected void setRootPath0(String rootPath) {
		checkNotNull(rootPath);
		checkArgument(!rootPath.isEmpty());

		this.rootPath = rootPath;
	}

	@Override
	public PathType getRootPathType() {
		return rootPathType==null ? DEFAULT_ROOT_PATH_TYPE : rootPathType;
	}

	@Override
	public void setRootPathType(PathType type) {
		checkNotLocked();

		setRootPathType0(type);
	}

	protected void setRootPathType0(PathType type) {
		checkNotNull(type);

		if(type==DEFAULT_ROOT_PATH_TYPE) {
			type = null;
		}

		rootPathType = type;
	}

	@Override
	public void setPathResolverManifest(PathResolverManifest pathResolverManifest) {
		checkNotLocked();

		setPathResolverManifest0(pathResolverManifest);
	}

	protected void setPathResolverManifest0(PathResolverManifest pathResolverManifest) {
		// NOTE setting the rootPath resolver manifest to null is legal
		// since the framework will use a default file based resolver in that case!
//		if (pathResolverManifest == null)
//			throw new NullPointerException("Invalid pathResolverManifest"); //$NON-NLS-1$

		this.pathResolverManifest = pathResolverManifest;
	}

	@Override
	public void forEachPathEntry(Consumer<? super PathEntry> action) {
		pathEntries.forEach(action);
	}

	@Override
	public void addPathEntry(PathEntry entry) {
		checkNotLocked();

		addPathEntry0(entry);
	}

	protected void addPathEntry0(PathEntry entry) {
		checkNotNull(entry);

		pathEntries.add(entry);
	}

	@Override
	public void removePathEntry(PathEntry entry) {
		checkNotLocked();

		removePathEntry0(entry);
	}

	protected void removePathEntry0(PathEntry entry) {
		checkNotNull(entry);

		pathEntries.remove(entry);
	}

	/**
	 * @see de.ims.icarus2.model.standard.manifest.AbstractManifest#setIsTemplate(boolean)
	 */
	@Override
	public void setIsTemplate(boolean isTemplate) {
		throw new ManifestException(ManifestErrorCode.MANIFEST_ILLEGAL_TEMPLATE_STATE,
				"Location manifest cannot be a template");
	}

	public static class PathEntryImpl implements PathEntry {

		private final PathType type;
		private final String value;

		public PathEntryImpl(PathType type, String value) {
			if (type == null)
				throw new NullPointerException("Invalid type"); //$NON-NLS-1$
			if (value == null)
				throw new NullPointerException("Invalid value"); //$NON-NLS-1$

			this.type = type;
			this.value = value;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int hash = 1;
			if(type!=null) {
				hash *= type.hashCode();
			}
			if(value!=null) {
				hash *= value.hashCode();
			}

			return hash;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if(this==obj) {
				return true;
			} if(obj instanceof PathEntry) {
				PathEntry other = (PathEntry) obj;
				return ClassUtils.equals(type, other.getType())
						&& ClassUtils.equals(value, other.getValue());
			}
			return false;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "PathEntry["+(type==null ? "<no_type>" : type.getStringValue())+"]@"+(value==null ? "<no_value>" : value); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		/**
		 * @see de.ims.icarus2.model.api.manifest.LocationManifest.PathEntry#getType()
		 */
		@Override
		public PathType getType() {
			return type;
		}

		/**
		 * @see de.ims.icarus2.model.api.manifest.LocationManifest.PathEntry#getValue()
		 */
		@Override
		public String getValue() {
			return value;
		}

	}
}
