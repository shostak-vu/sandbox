/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.xwiki.annotation.rest.internal;

import java.util.logging.Level;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.xwiki.annotation.AnnotationService;
import org.xwiki.annotation.rest.internal.model.jaxb.AnnotationRequestResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

/**
 * This class allow to do delete a single annotation.
 * 
 * @version $Id$
 */
@Component("org.xwiki.annotation.rest.internal.SingleAnnotationRESTResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/annotation/{id}")
public class SingleAnnotationRESTResource extends AbstractAnnotationService
{
    /**
     * The annotation service to be used by this REST interface.
     */
    @Requirement
    protected AnnotationService annotationService;

    /**
     * Deletes the specified annotation.
     * 
     * @param space the space of the document to delete the annotation from
     * @param page the name of the document to delete the annotation from
     * @param wiki the wiki of the document to delete the annotation from
     * @param id the id of the annotation to delete
     * @return a annotation response for which the response code will be 0 in case of success and non-zero otherwise
     */
    @DELETE
    public AnnotationRequestResponse doDelete(@PathParam("spaceName") String space, @PathParam("pageName") String page,
        @PathParam("wikiName") String wiki, @PathParam("id") String id)
    {
        try {
            DocumentInfo docInfo = getDocumentInfo(wiki, space, page, null, null, true, true);
            String documentName = docInfo.getDocument().getFullName();
            annotationService.removeAnnotation(documentName, id);

            AnnotationRequestResponse result = new AnnotationRequestResponse();
            result.setResponseCode(0);
            result.setSource(annotationService.getAnnotatedHTML(documentName).toString());
            result.getAnnotations().addAll(getAnnotationSet(annotationService.getAnnotations(documentName)));
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            AnnotationRequestResponse result = new AnnotationRequestResponse();
            result.setResponseCode(1);
            return result;
        }
    }
}
