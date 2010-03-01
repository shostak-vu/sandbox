package org.xwiki.it.ui.framework;

import org.openqa.selenium.WebDriver;

/**
 * Helper methods not related to a specific Page Object.
 *
 * @version $Id$ 
 * @since 2.3M1
 */
public class TestUtils
{
    public static void gotoPage(String space, String page, WebDriver driver)
    {
        gotoPage(space, page, "view", driver);
    }

    public static void gotoPage(String space, String page, String action, WebDriver driver)
    {
        gotoPage(space, page, "view", null, driver);
    }

    public static void gotoPage(String space, String page, String action, String queryString, WebDriver driver)
    {
        String url = getURLForPage(space, page, action, queryString);

        // Verify if we're already on the correct page and if so don't do anything
        if (!driver.getCurrentUrl().equals(url)) {
            driver.get(url);
        }
    }

    public static String getURLForPage(String space, String page, String action)
    {
        return getURLForPage(space, page, action, null);
    }

    public static String getURLForPage(String space, String page, String action, String queryString)
    {
        return "http://localhost:8080/xwiki/bin/" + action + "/" + space + "/" + page
            + (queryString == null ? "" : "?" + queryString);
    }

    public static boolean isOnPage(String space, String page, String action, WebDriver driver)
    {
        return driver.getCurrentUrl().equals(getURLForPage(space, page, action));
    }

    public static boolean isOnPage(String space, String page, WebDriver driver)
    {
        return isOnPage(space, page, "view", driver);
    }

    public static void deletePage(String space, String page, WebDriver driver)
    {
        TestUtils.gotoPage(space, page, "delete", "confirm=1", driver);
    }
}