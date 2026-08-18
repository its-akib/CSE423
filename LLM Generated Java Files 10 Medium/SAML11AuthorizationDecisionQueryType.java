
package org.keycloak.dom.saml.v1.protocol;

import org.keycloak.dom.saml.v1.assertion.SAML11ActionType;
import org.keycloak.dom.saml.v1.assertion.SAML11EvidenceType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Java class for AuthorizationDecisionQueryType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;complexType name="AuthorizationDecisionQueryType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:1.0:protocol}SubjectQueryAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:1.0:assertion}Evidence" minOccurs="0"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:1.0:assertion}Action" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="Resource" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class SAML11AuthorizationDecisionQueryType extends SAML11SubjectQueryAbstractType {

    protected List<SAML11ActionType> action = new ArrayList<>();
    protected SAML11EvidenceType evidence;
    protected URI resource;

    public URI getResource() {
        return resource;
    }

    public void setResource(URI resource) {
        this.resource = resource;
    }

    public SAML11EvidenceType getEvidence() {
        return evidence;
    }

    public void setEvidence(SAML11EvidenceType evidence) {
        this.evidence = evidence;
    }

    public void add(SAML11ActionType sadt) {
        action.add(sadt);
    }

    public boolean remove(SAML11ActionType sadt) {
        return action.remove(sadt);
    }

    public List<SAML11ActionType> get() {
        return Collections.unmodifiableList(action);
    }
}
