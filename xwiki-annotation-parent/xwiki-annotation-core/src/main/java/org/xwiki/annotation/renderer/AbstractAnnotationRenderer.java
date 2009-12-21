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
package org.xwiki.annotation.renderer;

import java.util.Collection;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.content.ContentAlterer;
import org.xwiki.annotation.internal.renderer.AnnotationGeneratorChainingListener;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.EmptyBlockChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.LinkLabelGenerator;

/**
 * Abstract class for annotation renderer, any specific syntax renderer should implement this class and provide the
 * specific annotation listener.
 * 
 * @version $Id$
 */
public abstract class AbstractAnnotationRenderer extends AbstractChainingPrintRenderer implements Initializable,
    AnnotationPrintRenderer
{
    /**
     * Selection cleaner so that the selection can be mapped on the content. <br />
     * TODO: not really sure if this is the right place for this pull, but the annotations generator is not a component
     * so it cannot 'require' it.
     */
    @Requirement("whitespace")
    protected ContentAlterer selectionAlterer;

    /**
     * Plain text parser used to parse generated link labels.
     */
    @Requirement("plain/1.0")
    protected StreamParser plainTextParser;

    /**
     * The annotations generator listener to use in this renderer.
     */
    protected AnnotationGeneratorListener annotationsGenerator;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        ListenerChain chain = new ListenerChain();
        setListenerChain(chain);

        // the annotation bookmarks to generate by the listener and pass to the renderer
        // FIXME: hack to be able to have the generator listener create the bookmarks and the next listener in chain
        // consume them, create an instance and pass it to both (instead of getting the results from first and passing
        // to second), using the hacky generator constructor
        AnnotationBookmarks bookmarks = new AnnotationBookmarks();
        annotationsGenerator =
            new AnnotationGeneratorChainingListener(getLinkLabelGenerator(), selectionAlterer, bookmarks, chain);
        // get the annotations print renderer and set its bookmarks
        AnnotationChainingPrintRenderer annotationsPrintRenderer = getAnnotationPrintRenderer(chain);
        annotationsPrintRenderer.setAnnotationBookmarks(bookmarks);

        // chain'em all
        // Construct the listener chain in the right order. Listeners early in the chain are called before listeners
        // placed later in the chain.
        chain.addListener(this);

        // empty block listener is needed by the label generator
        chain.addListener(new GeneratorEmptyBlockChainingListener(chain));
        // link label generator generates events for link labels automatically generated for empty links
        chain.addListener(new LinkLabelGeneratorChainingListener(getLinkLabelGenerator(), plainTextParser, chain));
        // annotations generator, chained to create the annotations events bookmarks
        chain.addListener((AnnotationGeneratorChainingListener) annotationsGenerator);
        // block state listener and empty block listeners needed by the XHTML renderer
        chain.addListener(new BlockStateChainingListener(chain));
        chain.addListener(new EmptyBlockChainingListener(chain));
        // the actual annotations renderer
        chain.addListener(annotationsPrintRenderer);
    }

    /**
     * @param chain the chain in which the renderer needs to be added.
     * @return the annotation listener to which the annotation events generated by this renderer should be sent (the
     *         actual renderer of annotated content)
     */
    public abstract AnnotationChainingPrintRenderer getAnnotationPrintRenderer(ListenerChain chain);

    /**
     * Getter for the link label generator to be used for generating link labels by this renderer.
     * 
     * @return the {@link LinkLabelGenerator} used to generate labels for links without labels by this renderer
     */
    public abstract LinkLabelGenerator getLinkLabelGenerator();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.annotation.renderer.AnnotationPrintRenderer#setAnnotations(java.util.Collection)
     */
    public void setAnnotations(Collection<Annotation> annotations)
    {
        this.annotationsGenerator.setAnnotations(annotations);
    }
}
