/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2020 Markus Gärtner <markus.gaertner@ims.uni-stuttgart.de>
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
/**
 *
 */
package de.ims.icarus2.query.api;

import de.ims.icarus2.Report;
import de.ims.icarus2.Report.ReportItem;

/**
 * Exception to signal one or more issues found during the processing of a
 * raw query payload, result instructions or groouping.
 *
 * @author Markus Gärtner
 *
 */
public class QueryProcessingException extends QueryException {

	private static final long serialVersionUID = 3726584869955100248L;

	//TODO make transient or ensure proper serialization of reports in general?
	private final Report<ReportItem> report;

	public QueryProcessingException(String message, Report<ReportItem> report) {
		super(QueryErrorCode.REPORT, report.toString(message));
		this.report = report;
	}

	public Report<ReportItem> getReport() {
		return report;
	}
}
