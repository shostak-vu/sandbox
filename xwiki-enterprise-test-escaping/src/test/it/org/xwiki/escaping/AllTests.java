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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Runs all functional escaping tests.
 * 
 * TODO
 * 1. start and stop the server
 * 2. actually download something from it
 * 3. add some basic escaping checks
 * 4. implement file excludes
 * 5. implement some manual test (copy requiring 2 parameters is a good example)
 * 6. add more sophisticated escaping tests -> parsing
 * 7. add over-escaping test
 * 
 * @version $Id$
 * @since 2.5
 */
@RunWith(ClasspathSuite.class)
public class AllTests
{
    /** Because junit disallows any references which persist between tests, there is a context which is static. */
//    private static PersistentTestContext context;

    @BeforeClass
    public static void init() throws Exception
    {
        HttpClient adminClient = new HttpClient();
        Credentials defaultcreds = new UsernamePasswordCredentials("Admin", "admin");
        adminClient.getState().setCredentials(AuthScope.ANY, defaultcreds);
//        context = new PersistentTestContext();
//        AbstractTest.setContext(context.getUnstoppable());
    }

    @AfterClass
    public static void shutdown() throws Exception
    {
//        context.shutdown();
    }
}
