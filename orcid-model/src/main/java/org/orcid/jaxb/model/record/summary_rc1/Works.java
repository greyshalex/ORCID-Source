/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.jaxb.model.record.summary_rc1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.orcid.jaxb.model.record_rc1.Group;
import org.orcid.jaxb.model.record_rc1.GroupsContainer;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "workGroup" })
@XmlRootElement(name = "works", namespace = "http://www.orcid.org/ns/activities")
public class Works implements GroupsContainer, Serializable {

    private static final long serialVersionUID = 3293976926416154039L;
    @XmlElement(name = "group", namespace = "http://www.orcid.org/ns/activities")
    private List<WorkGroup> workGroup;

    public List<WorkGroup> getWorkGroup() {
        if (workGroup == null)
            workGroup = new ArrayList<WorkGroup>();
        return workGroup;
    }

    @Override
    public Collection<? extends Group> retrieveGroups() {
        return getWorkGroup();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((workGroup == null) ? 0 : workGroup.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Works other = (Works) obj;
        if (workGroup == null) {
            if (other.workGroup != null)
                return false;
        } else if (!workGroup.equals(other.workGroup))
            return false;
        return true;
    }

}
