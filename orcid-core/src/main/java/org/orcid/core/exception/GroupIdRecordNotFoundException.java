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
package org.orcid.core.exception;

/**
 * 
 * @author Shobhit Tyagi
 * 
 */
public class GroupIdRecordNotFoundException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public GroupIdRecordNotFoundException() {
    }

    public GroupIdRecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupIdRecordNotFoundException(String message) {
        super(message);
    }

    public GroupIdRecordNotFoundException(Throwable cause) {
        super(cause);
    }

}
