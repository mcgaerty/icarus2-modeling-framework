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
package de.ims.icarus2.model.api.driver.mods;

/**
 * Specialized listener that can be passed on to certain methods of a
 * {@link DriverModule} to monitor its active task(s).
 *
 * @author Markus Gärtner
 *
 */
public interface ModuleMonitor {

	void start(DriverModule module);

	void progress(DriverModule module);

	void end(DriverModule module);

	void error(DriverModule module, Exception e);
}
