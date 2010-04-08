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
 *
 */

package com.xwiki.authentication;

import java.security.Principal;

import javax.servlet.http.Cookie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.realm.SimplePrincipal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Authenticate using IBM WebSphere Portal PUMA api.
 * 
 * @version $Id$
 */
public abstract class AbstractSSOAuthServiceImpl extends DefaultAuthServiceImpl
{
    /**
     * LogFactory <code>LOGGER</code>.
     */
    private static final Log LOG = LogFactory.getLog(AbstractSSOAuthServiceImpl.class);

    private static final String COOKIE_NAME = "XWIKISSOAUTHINFO";

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl#checkAuth(com.xpn.xwiki.XWikiContext)
     */
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        XWikiUser user = null;

        if (context.getRequest().getRemoteUser() != null) {
            user = checkAuthSSO(null, null, context);
        }

        if (user == null) {
            user = super.checkAuth(context);
        }

        return user;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl#checkAuth(java.lang.String, java.lang.String,
     *      java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        XWikiUser user = null;

        if (context.getRequest().getRemoteUser() != null) {
            user = checkAuthSSO(username, password, context);
        }

        if (user == null) {
            XWikiAuthService fallback = getFalback(context);

            if (fallback != null) {
                fallback.checkAuth(username, password, rememberme, context);
            }
        }

        return user;
    }

    public XWikiUser checkAuthSSO(String username, String password, XWikiContext context) throws XWikiException
    {
        Cookie cookie;

        System.out.println("checkAuth");

        System.out.println("Action: " + context.getAction());
        if (context.getAction().startsWith("logout")) {
            cookie = getCookie(COOKIE_NAME, context);
            if (cookie != null) {
                cookie.setMaxAge(0);
                context.getResponse().addCookie(cookie);
            }

            return null;
        }

        Principal principal = null;

        if (LOG.isDebugEnabled()) {
            Cookie[] cookies = context.getRequest().getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    System.out.println("CookieList: " + c.getName() + " => " + c.getValue());
                }
            }
        }

        cookie = getCookie(COOKIE_NAME, context);
        if (cookie != null) {
            System.out.println("Found Cookie");
            String uname = decryptText(cookie.getValue(), context);
            if (uname != null) {
                principal = new SimplePrincipal(uname);
            }
        }

        XWikiUser user;

        // Authenticate
        if (principal == null) {
            principal = authenticate(username, password, context);
            if (principal == null) {
                return null;
            }

            System.out.println("Saving auth cookie");
            String encuname = encryptText(principal.getName(), context);
            Cookie usernameCookie = new Cookie(COOKIE_NAME, encuname);
            usernameCookie.setMaxAge(-1);
            usernameCookie.setPath("/");
            context.getResponse().addCookie(usernameCookie);

            user = new XWikiUser(principal.getName());
        } else {
            user =
                new XWikiUser(principal.getName().startsWith(context.getDatabase()) ? principal.getName().substring(
                    context.getDatabase().length() + 1) : principal.getName());
        }

        System.out.println("XWikiUser=" + user);

        return user;
    }

    public Principal authenticate(String login, String password, XWikiContext context) throws XWikiException
    {
        Principal principal = null;

        String wikiName = context.getDatabase();

        // SSO authentication
        try {
            context.setDatabase(context.getMainXWiki());

            principal = authenticateSSOInContext(wikiName.equals(context.getMainXWiki()), context);
        } catch (XWikiException e) {
            // LOG.debug("Failed to authenticate with SSO", e);
            System.out.println("Failed to authenticate with SSO");
            e.printStackTrace();
        } finally {
            context.setDatabase(wikiName);
        }

        // Falback on configured authenticator
        if (principal == null) {
            XWikiAuthService fallback = getFalback(context);

            if (fallback != null) {
                getFalback(context).authenticate(login, password, context);
            }
        }

        return principal;
    }
    
    protected abstract Principal authenticateSSOInContext(boolean local, XWikiContext context) throws XWikiException;
}
