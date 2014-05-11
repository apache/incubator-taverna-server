package org.taverna.server.master.api;

import org.taverna.server.master.ContentsDescriptorBuilder;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * Description of properties supported by {@link RunREST}.
 * 
 * @author Donal Fellows
 */
public interface RunBean extends SupportAware {
	void setCdBuilder(ContentsDescriptorBuilder cdBuilder);

	void setRun(TavernaRun run);

	void setRunName(String runName);
}