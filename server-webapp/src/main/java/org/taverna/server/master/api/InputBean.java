package org.taverna.server.master.api;

import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.ContentsDescriptorBuilder;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.utils.FilenameUtils;

/**
 * Description of properties supported by {@link InputREST}.
 * 
 * @author Donal Fellows
 */
public interface InputBean extends SupportAware {
	TavernaServerInputREST connect(TavernaRun run, UriInfo ui);

	void setCdBuilder(ContentsDescriptorBuilder cd);

	void setFileUtils(FilenameUtils fn);
}