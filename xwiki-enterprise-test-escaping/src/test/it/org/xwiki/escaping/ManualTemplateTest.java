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

import java.io.IOException;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.escaping.framework.AbstractEscapingTest;
import org.xwiki.escaping.framework.AbstractManualTest;
import org.xwiki.escaping.framework.XMLEscapingValidator;


/**
 * Runs additional escaping tests that need more complex manual setup. These tests are missed by the
 * automatic test builder.
 * 
 * @version $Id$
 * @since 2.5
 */
public class ManualTemplateTest extends AbstractManualTest
{
    /**
     * Initialize tests.
     */
    @BeforeClass
    public static void init()
    {
        // for tests using "language" parameter
        AbstractEscapingTest.setMultiLanguageMode(true);
    }

    /**
     * Shutdown tests
     */
    @AfterClass
    public static void shutdown()
    {
        // restore single language mode
        AbstractEscapingTest.setMultiLanguageMode(false);
    }

    // already covered
    // XWIKI-5205
    // XWIKI-5209
    // XWIKI-5193 (space)
    // XWIKI-5344 (space)
    // XWIKI-5204
    // testImported()

    // too complicated to cover
    // testVersionSummary()

    @Test
    public void testEditReflectedXSS()
    {
        if (!initialize("templates/htmlheader.vm", null)) {
            return;
        }
        checkUnderEscaping(createUrl("edit", "Main", XMLEscapingValidator.getTestString(), null), "XWIKI-4758");
    }

    @Test
    public void testErrorTraceEscaping()
    {
        if (!initialize("templates/exceptioninline.vm", null)) {
            return;
        }
        checkUnderEscaping(createUrl("viewrev", "Main", "WebHome", params(test("rev"))), "XWIKI-5170 error trace");
    }

    @Test
    public void testEditorEscaping()
    {
        if (!initialize("templates/edit.vm", null)) {
            return;
        }
        // tests for XWIKI-5164, XML symbols in editor parameter should be escaped
        checkUnderEscaping(createUrl("edit", "Main", "Page", params(test("editor"))), "XWIKI-5164 editor");
        checkUnderEscaping(createUrl("edit", "Main", "Page", params(kv("editor", "wysiwyg"), test("section"))),
            "XWIKI-5164 section");
        checkUnderEscaping(createUrl("edit", "Main", "Page", params(kv("editor", "wiki"), test("x-maximized"))),
            "XWIKI-5164 x-maximized");
    }

    @Test
    public void testAdminEditor()
    {
        if (!initialize("templates/admin.vm", null)) {
            return;
        }
        checkUnderEscaping(createUrl("admin", "XWiki", "AdminSheet", params(test("editor"))),
            "XWIKI-5190 admin editor");
        // same page after redirect
        checkUnderEscaping(createUrl("view", "Main", "WebHome", params(kv("xpage", "admin"), test("editor"))),
            "XWIKI-5190 admin editor redirect");
    }

    @Test
    public void testAdminSection()
    {
        if (!initialize("templates/admin.vm", null)) {
            return;
        }
        // kind of covered (only the redirect version)
        checkUnderEscaping(createUrl("admin", "XWiki", "AdminSheet", params(test("section"))),
            "XWIKI-5190 admin section");
        // same page after redirect
        checkUnderEscaping(createUrl("view", "Main", "WebHome", params(kv("xpage", "admin"), test("section"))),
            "XWIKI-5190 admin section redirect");
    }

    @Test
    public void testAttachmentsInline()
    {
        if (!initialize("templates/attachments*.vm", null)) {
            return;
        }
        // need a page with attachments, Sandbox has an image attached by default
        checkUnderEscaping(createUrl("view", "Sandbox", "WebHome", params(kv("viewer", "attachments"), test("xredirect"))),
            "XWIKI-5191 attachments inline");
    }

    @Test
    public void testBrowseWysiwygSQL() throws IOException
    {
        if (!initialize("templates/browsewysiwyg.vm", null)) {
            return;
        }
        // TODO check for SQL escaping (i.e. additionally put \ and ;)
        String url = createUrl("view", "Sandbox", "WebHome", params(kv("xpage", "browsewysiwyg"), test("text")));
        checkUnderEscaping(url, "XWIKI-5193 sql");
        checkForErrorTrace(url);
    }

    @Test
    public void testBrowseWysiwygPage()
    {
        // also covers former testBrowseWysiwygPageLink()
        if (!initialize("templates/browsewysiwyg.vm", null)) {
            return;
        }
        // need an existing page with name = title = test string
        createPage("Main", XMLEscapingValidator.getTestString(), XMLEscapingValidator.getTestString(), "Bla bla");
        checkUnderEscaping(createUrl("view", "Main", "Test", params(template("browsewysiwyg"))),
            "XWIKI-5193 page");
    }

    @Test
    public void testWysiwygRecentViewsPage()
    {
        if (!initialize("templates/recentdocwysiwyg.vm", null)) {
            return;
        }
        // need an existing page with name = title = test string
        createPage("Main", XMLEscapingValidator.getTestString(), XMLEscapingValidator.getTestString(), "Bla bla");
        checkUnderEscaping(createUrl("view", "Main", "Test", params(template("recentdocwysiwyg"))),
            "XWIKI-5193 recent docs");
    }

    @Test
    public void testSearchWysiwygSQL() throws IOException
    {
        if (!initialize("templates/searchwysiwyg.vm", null)) {
            return;
        }
        // TODO check for SQL escaping (i.e. additionally put \ and ;)
        String spaceUrl = createUrl("view", "Main", "Test", params(kv("xpage", "searchwysiwyg"), test("space")));
        checkUnderEscaping(spaceUrl, "XWIKI-5344 sql space");
        checkForErrorTrace(spaceUrl);

        String pageUrl = createUrl("view", "Main", "Test", params(kv("xpage", "searchwysiwyg"), test("page")));
        checkUnderEscaping(pageUrl, "XWIKI-5344 sql page");
        checkForErrorTrace(pageUrl);
    }

    @Test
    public void testSearchWysiwygPageLink()
    {
        if (!initialize("templates/searchwysiwyg.vm", null)) {
            return;
        }
        // need an existing page with name = title = test string
        createPage("Main", XMLEscapingValidator.getTestString(), XMLEscapingValidator.getTestString(), "Bla bla");
        checkUnderEscaping(createUrl("view", "Main", "Test", params(template("searchwysiwyg"))),
            "XWIKI-5344 page link");
    }

    @Test
    public void testLoginRedirect()
    {
        if (!initialize("templates/login.vm", null)) {
            return;
        }
        // need to be logged off
        setLoggedIn(false);
        try {
            checkUnderEscaping(createUrl("login", "XWiki", "XWikiLogin", params(test("xredirect"))),
                "XWIKI-5246 xredirect");
        } finally {
            setLoggedIn(true);
        }
    }

    @Test
    public void testLoginSrid()
    {
        if (!initialize("templates/login.vm", null)) {
            return;
        }
        // need to be logged off
        setLoggedIn(false);
        try {
            checkUnderEscaping(createUrl("login", "XWiki", "XWikiLogin", params(test("srid"))),
                "XWIKI-5246 srid");
        } finally {
            setLoggedIn(true);
        }
    }

    @Test
    public void testEditActions()
    {
        if (!initialize("edit comment", null)) {
            return;
        }
        // need an existing page with name = title = test string
        createPage("Main", XMLEscapingValidator.getTestString(), XMLEscapingValidator.getTestString(), "Bla bla");
        checkUnderEscaping(createUrl("edit", "Main", "WebHome", params(kv("editor", "wiki"), test("comment"))),
            "edit comment");
    }

    @Test
    public void testCreateEditMode()
    {
        if (!initialize("templates/create.vm", null)) {
            return;
        }
        checkUnderEscaping(createUrl("edit", "Main", XMLEscapingValidator.getTestString(),
            params(template("createinline"))), "XWIKI-5207 create inline");
        checkUnderEscaping(createUrl("edit", "Main", XMLEscapingValidator.getTestString(),
            params(template("create"), kv("ajax", "1"))), "XWIKI-5207 create ajax");
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

    @Test
    public void testCopyExistingPage()
    {
        if (!initialize("templates/copy.vm", null)) {
            return;
        }
        // need an existing page with name = test string
        createPage("Main", XMLEscapingValidator.getTestString(), "", "Bla bla");
        checkUnderEscaping(createUrl("view", "Main", XMLEscapingValidator.getTestString(),
            params(kv("xpage", "copy"))), "XWIKI-5206 copy existing page");
    }

    /**
     * Run escaping tests for copy.vm.
     * 
     * @param parameter parameter to test
     */
    private void testCopy(String parameter)
    {
        if (!initialize("templates/copy.vm", null)) {
            return;
        }
        // XWIKI-5206
        // copy.vm does not display the form if targetdoc is not set
        String url = createUrl(null, null, null, params(template("copy"), test("targetdoc"), test(parameter)));
        // delete the copy afterwards
        deleteAfterwards(null, XMLEscapingValidator.getTestString());
        checkUnderEscaping(url, "\"" + parameter + "\"");
    }

    @Test
    public void testRename()
    {
        if (!initialize("templates/rename.vm", null)) {
            return;
        }
        // rename.vm is only used with step=2, otherwise renameStep1.vm is used
        String[] tested = new String[] {"language", "sourcedoc", "targetdoc",
                                        "newPageName", "newSpaceName", "parameterNames"};
        // test page will probably be created
        deleteAfterwards(null, XMLEscapingValidator.getTestString());
        for (String parameter : tested) {
            // make sure the target page exists (cannot use WebHome, since it might be renamed)
            createPage(null, "testRenameSource" + System.nanoTime(), "test", "test");
            Map<String, String> params = params(template("rename"), kv("step", "2"), test(parameter));
            // HTTP 400 is returned if newPageName is empty, 409 if the new page exist
            if (!params.containsKey("newPageName")) {
                String page = "testRename" + System.nanoTime();
                params.put("newPageName", page);
                // the test may create a page, schedule for deletion
                deleteAfterwards(null, page);
            }
            String url = createUrl(null, null, null, params);
            checkUnderEscaping(url, "\"" + parameter + "\"");
        }
    }
}

