package org.taverna.server.master.common;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Element;

/**
 * Encapsulation of a T2flow document.
 * 
 * @author dkf
 */
@XmlRootElement(name = "workflow")
@XmlType(name = "Workflow")
public class Workflow {
	/**
	 * Literal document.
	 */
	@XmlAnyElement(lax = true)
	public Element[] content;
}
