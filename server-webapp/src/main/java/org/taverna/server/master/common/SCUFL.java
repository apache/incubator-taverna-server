package org.taverna.server.master.common;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Element;

/**
 * Encapsulation of a SCUFL document.
 * 
 * @author dkf
 */
@XmlRootElement(name = "scufl")
@XmlType(name = "SCUFL")
public class SCUFL extends Uri {
	/**
	 * Literal document.
	 */
	// TODO Use the real definition of SCUFL
	@XmlAnyElement(lax = true)
	public Element[] content;
}
