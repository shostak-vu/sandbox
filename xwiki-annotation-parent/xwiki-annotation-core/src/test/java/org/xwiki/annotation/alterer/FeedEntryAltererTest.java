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

package org.xwiki.annotation.alterer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xwiki.annotation.ContentAlterer;
import org.xwiki.annotation.internal.content.AlteredContent;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Test of piped alterations.
 * 
 * @version $Id$
 */
public class FeedEntryAltererTest extends AbstractComponentTestCase
{
    /**
     * Content alterer tested by this suite.
     */
    private ContentAlterer compositeAlterer;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractComponentTestCase#setUp()
     */
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        compositeAlterer = getComponentManager().lookup(ContentAlterer.class, "FEEDENTRY");
    }

    /**
     * Basic test for feed entry alterer, composing HTML and plain text altering.
     */
    @Test
    public void testAlter()
    {
        /*
         * 012345678901234567890123456789012345678 <a>luc_ien</a>df dsf<b>sd<c>qs@d</c>s-q</b> luc_ien df dsf sd qs@d
         * s-q 0123456 789012 34 5678 901 luc ien df dsf sd qs d s q 012 345 67 890 12 34 5 6 7
         */
        String input = "<a>luc_ien</a>df dsf<b>sd<c>qs@d</c>s-q</b>";
        AlteredContent ac2 = compositeAlterer.alter(input);

        assertEquals(17, ac2.getInitialOffset(8));
        assertEquals(8, ac2.getAlteredOffset(17));

        assertEquals(8, ac2.getAlteredOffset(16));

        assertEquals(2, ac2.getAlteredOffset(5));
        assertEquals(3, ac2.getAlteredOffset(6));
        assertEquals(3, ac2.getAlteredOffset(7));

        assertEquals(0, ac2.getAlteredOffset(3));
        assertEquals(0, ac2.getAlteredOffset(2));
        assertEquals(0, ac2.getAlteredOffset(1));
        assertEquals(0, ac2.getAlteredOffset(0));
    }
}
