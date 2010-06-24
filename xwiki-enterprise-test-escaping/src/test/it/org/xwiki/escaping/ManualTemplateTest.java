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

package org.xwiki.escaping;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.escaping.framework.AbstractEscapingTest;
import org.xwiki.escaping.framework.XMLEscapingValidator;


/**
 * Runs additional escaping tests that need more complex manual setup. These tests are missed by the
 * automatic test builder.
 * 
 * @version $Id$
 * @since 2.5
 */
public class ManualTemplateTest extends AbstractEscapingTest
{
    /**
     * Create new ManualTemplateTest
     */
    public ManualTemplateTest()
    {
        super(Pattern.compile(".*\\.vm"));
    }

    @Test
    public void testCopySourcedoc()
    {
        testCopy("sourcedoc");
    }

    @Test
    public void testCopyLanguage()
    {
        testCopy("language");
    }

    /**
     * Run escaping tests for copy.vm.
     * 
     * @param parameter parameter to test
     */
    private void testCopy(String parameter)
    {
        // copy.vm does not display the form if targetdoc is not set
        Assert.assertTrue("Initialization failed", initialize("templates/copy.vm", null));
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("xpage", "copy");
        params.put("targetdoc", "bla");
        params.put(parameter, XMLEscapingValidator.getTestString());
        String url = createUrl(null, null, null, params);
        checkUnderEscaping(url, "\"" + parameter + "\"");
    }

    /**
     * {@inheritDoc}
     * 
     * The file reader is not used in manual tests, we just return a fixed set of parameters.
     * 
     * @see org.xwiki.escaping.TemplateTest#parse(java.io.Reader)
     */
    @Override
    protected Set<String> parse(Reader reader)
    {
        Set<String> parameters = new HashSet<String>();
        parameters.add("language");
        parameters.add("sourcedoc");
        parameters.add("targetdoc");
        parameters.add("step"); // = 2
        parameters.add("newPageName");
        parameters.add("newSpaceName");
        parameters.add("parameterNames");
        return parameters;
    }
}

