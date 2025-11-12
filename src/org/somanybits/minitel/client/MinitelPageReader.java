/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.somanybits.minitel.TeletelCode;

/**
 *
 * @author eddy
 */
public class MinitelPageReader {

    private int port = 0;
    private String domain;
    private Page page;

    TagInfo roottag = null;
    TagInfo currenttag = null;

    public static final int MINITEL_TAG_DEPTH = 3;

    private TeletelCode tc = new TeletelCode();

    public MinitelPageReader(String domain, int port) {
        this.port = port;
        this.domain = domain;
    }



    public Page get(String url) throws IOException {

        if (!isHttpHostOrHostPort(url)) {
            url = "http://" + domain + ":" + Integer.toString(port) + "/" + url;
        }

        page = new Page(Page.MODE_40_COL);
        roottag = null;
        currenttag = null;

        // READ LINK
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Minitel/5.0 (Java JSoup)")
                    .timeout(15_000)
                    .get();
        } catch (IOException ex) {
            page.addData(("Error:" + ex.getMessage()).getBytes());
            return page;
        }

        NodeTraversor.traverse(new NodeVisitor() {
            TagInfo currentTag = null;

            @Override
            public void head(Node node, int depth) {
                if (node instanceof Element el) {

                    Map<String, String> attrs = new LinkedHashMap<>();

                    for (Attribute a : el.attributes()) {
                        attrs.put(a.getKey(), a.getValue());
                    }
                    if (depth >= MINITEL_TAG_DEPTH) {
                        buildTreeTagInfo(el.normalName(), depth, attrs, el.nodeValue());
                    }

                }
            }

            @Override
            public void tail(Node node, int depth) {
            }
        }, doc);

        analyseTageInfoTree(roottag);

        return page;
    }
    
        private void buildTreeTagInfo(String tagname, int depth, Map<String, String> attr, String value) {

        TagInfo ti = new TagInfo(tagname, depth, attr, value);
        System.err.println("tag=" + tagname + " depth=" + depth);

        if (ti.depth == MINITEL_TAG_DEPTH) {
            if (roottag == null) {
                roottag = ti;
                currenttag = ti;
            } else {
                System.out.println("can't create no more Minitel page");
            }
        } else if (ti.depth == currenttag.depth + 1) {
            ti.parent = currenttag;
            currenttag.children.add(ti);
            currenttag = ti;

        } else if (ti.depth == currenttag.depth) {
            currenttag = currenttag.parent;

            ti.parent = currenttag;
            currenttag.children.add(ti);
            currenttag = ti;
        } else if (ti.depth == (currenttag.depth - 1)) {
            currenttag = currenttag.parent;
            currenttag = currenttag.parent;

            ti.parent = currenttag;
            currenttag.children.add(ti);
            currenttag = ti;
        } else {
            System.out.println("Depth Error !!");
        }

    }
    //static int level = 0;

    private void analyseTageInfoTree(TagInfo ti) throws IOException {

        System.out.println(">>>" + ti.tagname.indent((ti.depth - MINITEL_TAG_DEPTH) * 2).replace("\n", "") + " -> " + ti.attr);

        if (ti.tagname.equals("minitel")) {
            page.addData(tc.clear());
        }

        for (int i = 0; i < ti.children.size(); i++) {
            switch (ti.tagname) {

                case "div" -> {
                    readDivTag(ti);
//                    level--;
                    return;
                }
                case "menu" -> {
                    readMenuTag(ti);
//                    level--;
                    return;
                }
                case "br" -> {
                    page.addData("\r\n".getBytes());
//                    level--;
                    return;
                }

                default -> {

                }
            }
            analyseTageInfoTree(ti.children.get(i));
        }

//        level--;
    }
    private static final Pattern HTTP_HOST_PORT = Pattern.compile(
            "^http://(?:"
            + "localhost"
            + "|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(?:\\.(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?))*"
            + "|\\d{1,3}(?:\\.\\d{1,3}){3}"
            + ")"
            + "(?::(6553[0-5]|655[0-3]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}))?"
            + "/?$"
    );

    public static boolean isHttpHostOrHostPort(String s) {
        return HTTP_HOST_PORT.matcher(s).matches();
    }

    private void readMinitelTag(Element elmt) throws IOException {

        this.page.setTile(elmt.attr("title"));

    }

    private void readDivTag(TagInfo ti) throws IOException {

        int x = ti.attr.get("left") != null ? Integer.parseInt(ti.attr.get("left")) : 0;
        int y = ti.attr.get("top") != null ? Integer.parseInt(ti.attr.get("top")) : 0;

        System.out.println("x=" + x + " y=" + y);
        
        
        
        for (int i = 0; i < ti.children.size(); i++) {

            TagInfo child = ti.children.get(i);
            System.out.println(child.tagname + "->" + child.attr);

            switch (child.tagname) {
                case "row" -> {
                    page.addData(tc.setCursor(x, y));
                    page.addData(child.value.getBytes());
                    y++;
                }
                case "br" -> {
                    page.addData(tc.setCursor(x, y));
                    y++;
                }
            }
        }

    }

    private void readMenuTag(TagInfo ti) throws IOException {

        int x = ti.attr.get("left") != null ? Integer.parseInt(ti.attr.get("left")) : 0;
        int y = ti.attr.get("top") != null ? Integer.parseInt(ti.attr.get("top")) : 0;

        String keytype = ti.attr.get("keytype");
        char count = '0';
        if (keytype != null) {
            switch (keytype) {

                case "alpha":
                    count = 'A';
                    break;

                case "number":

                default:
                    count = '0';

            }

        }

        System.out.println("x=" + x + " y=" + y);

        for (int i = 0; i < ti.children.size(); i++) {
            TagInfo child = ti.children.get(i);
            System.out.println(child.tagname + "->" + child.attr);
            switch (child.tagname) {
                case "item" -> {
                    page.addData(tc.setCursor(x, y));
                    page.addData((count + ") " + child.value).getBytes());

                    page.addMenu(count + "", child.attr.get("link"));
                    count++;
                    y++;
                }
                case "br" -> {
                    page.addData(tc.setCursor(x, y));
                    y++;
                }
            }
        }

    }

    private class TagInfo {

        TagInfo parent = null;
        ArrayList<TagInfo> children = new ArrayList<>();
        Map<String, String> attr;

        String tagname;
        int depth;
        String value;

        public TagInfo(String tagname, int depth, Map<String, String> attr, String value) {
            this.tagname = tagname;
            this.depth = depth;
            this.attr = attr;
            this.value = value;
        }
    }

}
