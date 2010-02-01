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

package org.xwiki.annotation;

import java.util.Collection;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Component responsible for providing annotations related services: the management of annotations (retrieving, adding,
 * removing, updating) and rendering them on their respective targets. <br />
 * This service does not parse or interpret the references of targets its operating with, caller is responsible to be
 * consistent in calls and use references which are interpreted by the used implementations for
 * {@link org.xwiki.annotation.io.IOService and org.xwiki.annotation.io.IOTargetService}. <br />
 * 
 * @version $Id$
 * @see {@link org.xwiki.annotation.io.IOTargetService}, {@link org.xwiki.annotation.io.IOTargetService}
 */
@ComponentRole
public interface AnnotationService
{
    /**
     * Returns the XHTML of the requested source, along with annotations inserted as {@code span} elements inside it.
     * It's a particular case of {@link #getAnnotatedRenderedContent(String, String, String)} for unspecified input
     * syntax and {@code xhtml/1.0} output syntax.
     * 
     * @param sourceReference reference to the source to be rendered in XHTML with annotations
     * @return rendered and annotated document
     * @throws AnnotationServiceException if anything goes wrong retrieving or rendering the requested source
     * @see #getAnnotatedRenderedContent(String, String, String)
     */
    String getAnnotatedHTML(String sourceReference) throws AnnotationServiceException;

    /**
     * Returns result obtained by rendering with annotations the source referenced by the {@code sourceReference},
     * parsed in {@code sourceSyntax}.
     * 
     * @param sourceReference the reference to the source to be rendered in XHTML with annotations
     * @param sourceSyntax the syntax to parse the source in. If this parameter is null, the default source syntax will
     *            be used, as returned by the target IO service.
     * @param outputSyntax the syntax to render in (e.g. "xhtml/1.0")
     * @return the annotated rendered source
     * @throws AnnotationServiceException if anything goes wrong retrieving or rendering the requested source
     */
    String getAnnotatedRenderedContent(String sourceReference, String sourceSyntax, String outputSyntax)
        throws AnnotationServiceException;

    /**
     * Adds an the specified annotation for the specified target.
     * 
     * @param target serialized reference of the target of the annotation
     * @param selection HTML selection concerned by annotations
     * @param selectionContext HTML selection context
     * @param offset offset of the selection in context
     * @param user the author of the annotation
     * @param metadata annotation content
     * @throws AnnotationServiceException if selection resolution fail or if an XWikiException occurred
     */
    void addAnnotation(String target, String selection, String selectionContext, int offset, String user,
        String metadata) throws AnnotationServiceException;

    /**
     * Remove an annotation given by its identifier, which should be unique among all annotations on the same target.
     * 
     * @param target the string serialized reference to the content on which the annotation is added
     * @param annotationID annotation identifier
     * @throws AnnotationServiceException if anything goes wrong accessing the annotations store
     */
    void removeAnnotation(String target, String annotationID) throws AnnotationServiceException;

    /**
     * Returns all the annotations on the passed content.
     * 
     * @param target the string serialized reference to the content for which to get the annotations
     * @return all annotations which target the specified content
     * @throws AnnotationServiceException if anything goes wrong accessing the annotations store
     */
    Collection<Annotation> getAnnotations(String target) throws AnnotationServiceException;

    /**
     * Returns the annotation identified by {@code id} on the specified target.
     * 
     * @param target the serialized reference to the content on which the annotation is added
     * @param id the identifier of the annotation
     * @return the annotation identified by {@code id}
     * @throws AnnotationServiceException if anything goes wrong accessing the annotations store
     */
    Annotation getAnnotation(String target, String id) throws AnnotationServiceException;

    /**
     * Shortcut function to get all annotations which are valid on the specified target, regardless of the updates the
     * document and its annotations suffered from creation ('safe' or 'updated' state).
     * 
     * @param target the string serialized reference to the content for which to get the annotations
     * @return all annotations which are valid on the specified content
     * @throws AnnotationServiceException if anything goes wrong accessing the annotations store
     * @see {@link org.xwiki.annotation.maintainer.AnnotationState}
     */
    Collection<Annotation> getValidAnnotations(String target) throws AnnotationServiceException;
}
