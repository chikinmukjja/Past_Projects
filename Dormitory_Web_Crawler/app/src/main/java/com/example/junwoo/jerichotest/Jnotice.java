package com.example.junwoo.jerichotest;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by junwoo on 2015-12-30.
 */
public class Jnotice {

    private int boardNum = 0;
    private String title = null;
    private String link = null;
    private String user = null;
    private String date = null;
    private int clicks = 0;
    private String content = null;

    public int getBoardNum() {
        return boardNum;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getUser() {
        return user;
    }

    public String getDate() {
        return date;
    }

    public int getClicks() {
        return clicks;
    }

    public boolean setBoardNum(String boardNum) {
        if(boardNum.length() > 0 ) {
           // System.out.println("-"+boardNum+"-");
            this.boardNum = Integer.parseInt(boardNum);
            return true;
        }
        else {
            this.boardNum = -1;
            return false;
        }
    }

    public boolean setTitle(String title) {
        if(title != null) {
            this.title = title;
            return true;
        }
        else return false;
    }

    public boolean setClicks(String clicks) {
        if(clicks.length() > 0) {
            this.clicks = Integer.parseInt(clicks);
            return true;
        }
        else {
            this.clicks = -1;
            return false;
        }
    }

    public boolean setDate(String date) {
        if(date != null) {
            this.date = date;
            return true;
        }
        else return false;
    }

    public boolean setLink(String link) {
        if(link != null) {
            this.link = link;
            return true;
        }
        else return false;
    }

    public boolean setUser(String user) {
        if(user != null) {
            this.user = user;
            return true;
        }
        else return false;
    }

    public String getLinkContent(String link)
    {

        String content = null;
        Source source = null;
        String url = "http://seoul.jbdream.or.kr";
        String subLink = link.substring(2); // ../~~ 에서 .. 생략


        if(this.content != null)return this.content;

        try {

            URL URLNotice = new URL(url + subLink);
            source = new Source(URLNotice);

            List<Element> SpanTags = source.getAllElements(HTMLElementName.SPAN);

            //System.out.println("here ! "+SpanTags.size());
            for(int i = 0;i < SpanTags.size();i++){
                Element spanElement = SpanTags.get(i);
                if(spanElement.getAttributes().toString().contains("writeContents")) {
                    List<Element> divTags = spanElement.getAllElements(HTMLElementName.DIV);
                    content = new String(spanElement.getTextExtractor().toString());

                }
            }

        }catch(IOException e) {

        }


        return content;
    }
}
