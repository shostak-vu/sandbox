package com.xpn.xwiki.watch.client.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.gwt.api.client.XObject;
import com.xpn.xwiki.watch.client.Constants;
import com.xpn.xwiki.watch.client.annotation.Annotation;

import java.util.List;
import java.util.ArrayList;


/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */

public class FeedArticle implements IsSerializable {
    protected String pageName;
    protected String title;
    protected String feedName;
    protected String feedURL;
    protected String url;
    protected String author;
    protected String date;
    protected String content;
    protected List<String> tags = new ArrayList<String>();
    protected int flagStatus;
    protected int readStatus;
    protected List<FeedArticleComment> comments = new ArrayList<FeedArticleComment>();
    protected List<Annotation> annotations = new ArrayList<Annotation>();

    public FeedArticle() {
    }

    public FeedArticle(Document article) {
        XObject feedentry = article.getObject("XWiki.FeedEntryClass", 0);
        pageName = article.getFullName();
        title = feedentry.getViewProperty("title");
        feedName = feedentry.getViewProperty("feedname");
        feedURL = feedentry.getViewProperty("feedurl");
        url = feedentry.getViewProperty("url");
        author = feedentry.getViewProperty("author");
        date = feedentry.getViewProperty("date");
        content = feedentry.getViewProperty("content");
        // split the tags string and get the list
        String tagsString = feedentry.getViewProperty("tags");
        this.tags.clear();
        this.tags.addAll(parseTagsString(tagsString, true));
        Integer iFlag = (Integer) feedentry.getProperty("flag");
        flagStatus = (iFlag==null) ? 0 : iFlag.intValue();
        Integer iRead = (Integer) feedentry.getProperty("read");
        readStatus = (iRead==null) ? 0 : iRead.intValue();
        List<XObject> commentList = article.getObjects("XWiki.XWikiComments");
        if (commentList!=null) {
            for (int i=0;i<commentList.size();i++) {
                XObject comment = (XObject) commentList.get(i);
                String comment_date = comment.getViewProperty("date");
                String comment_author = comment.getViewProperty("author").replaceAll("XWiki.", "");
                String comment_content = comment.getViewProperty("comment");
                comments.add(new FeedArticleComment(comment_author, comment_date, comment_content));
            }
        }
    }
    
    public static List<String> parseTagsString(String tagsString, boolean removeDuplicates) {
        List<String> tagsList = new ArrayList<String>();
        if (tagsString != null) {
            String[] tagarray = tagsString.split(Constants.PROPERTY_TAGS_SEPARATORS_EDIT);
            for (int i = 0; i < tagarray.length; i++) {
                if (tagarray[i].trim().length() > 0 && (!tagsList.contains(tagarray[i].trim()))) {
                    tagsList.add(tagarray[i].trim());
                }
            }
        }
        return tagsList;
    }
    
    public static List<String> joinTagsLists(List<String> l1, List<String> l2, boolean removeDuplicates) {
        List<String> newList = new ArrayList<String>();
        newList.addAll(l1);
        if (removeDuplicates) {
            for (int i = 0; i < l2.size(); i++) {
                if (!newList.contains(l2.get(i))) {
                    newList.add(l2.get(i));
                }
            }
        } else {
            newList.addAll(l2);
        }
        return newList;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getFeedURL() {
        return feedURL;
    }

    public void setFeedURL(String feedURL) {
        this.feedURL = feedURL;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getFlagStatus() {
        return flagStatus;
    }

    public void setFlagStatus(int flagStatus) {
        this.flagStatus = flagStatus;
    }

    public int getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(int readStatus) {
        this.readStatus = readStatus;
    }

    public List<FeedArticleComment> getComments() {
        return comments;
    }

    public void setComments(List<FeedArticleComment> comments) {
        this.comments = comments;
    }

    public int getCommentsNumber() {
        if (comments==null)
         return 0;
        else
         return comments.size();
    }

    public String getPageName() {
        return pageName;
    }
    
    public List<Annotation> getAnnotations()
    {
    	return annotations;
    }

    /**
     * @param annotations the annotations to set
     */
    public void setAnnotations(List<Annotation> annotations)
    {
        this.annotations.clear();
        this.annotations.addAll(annotations);
    }
}
