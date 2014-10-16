package uk.ac.ebi.interpro.common;

import java.io.*;
import java.util.*;

/**
 * Usage:
 * <p/>
 * HTMLGenerator html = new HTMLGenerator(pw);
 * <p/>
 * For instance in a servlet:
 * <p/>
 * res.setContentType("text/html");
 * PrintWriter pw = res.getWriter();
 * <p/>
 * Print an element containing element (most general case)
 * <p/>
 * html.someTag.openTag(someParameters).setSomeAttributes().close();
 * printSomeMoreElements
 * html.someTag.end()
 * <p/>
 * Print an element containing text with custom attributes
 * <p/>
 * html.someElement.openTag(someParameters).setSomeAttributes().close().printText(SomeText).end()
 * <p/>
 * Print an element containing text with standard attributes
 * <p/>
 * html.someTag.start(someParameters).printText(someText).end()
 * <p/>
 * Print an element containing text with no attributes
 * <p/>
 * html.someTag.print(someText)
 * <p/>
 * Print an empty element
 * <p/>
 * html.someTag.print()
 */
public class HTMLGenerator {

/* This is the public interface: a set of tags which can be used to generate markup */

    public HTMLElement html = new HTMLElement("html");
    public HeadElement head = new HeadElement();
    public HTMLElement body = new HTMLElement("body");
    public InputElement textbox = new InputElement("text");
    public InputElement hidden = new InputElement("hidden");
    public CheckboxElement checkbox = new CheckboxElement();
    public TextAreaElement textarea = new TextAreaElement();
    public InputElement submit = new InputElement("submit");
    public FormElement form = new FormElement();
    public HTMLElement br = new HTMLElement("br");
    public HTMLElement code = new HTMLElement("code");
    public HTMLElement span = new HTMLElement("span");
    public HTMLElement div = new HTMLElement("div");
    public HTMLElement p = new HTMLElement("p");
    public TableElement table = new TableElement();
    public HTMLElement tr = new HTMLElement("tr");
    public TDElement td = new TDElement();
    public LinkElement link = new LinkElement();
    public HTMLElement col = new HTMLElement("col");
    public HTMLElement h1 = new HTMLElement("h1");
    public HTMLElement b = new HTMLElement("b");
    public ImageElement img = new ImageElement();
    public BaseElement base = new BaseElement();


/* Internals */

    private boolean pretty = true;
    private boolean tagOpen = false;
    private Stack elements = new Stack();
    private StringBuffer margin = new StringBuffer();
    private PrintWriter pw;

    private String hoverOnMouseOver="",hoverOnMouseOut="";

    public void setHover(String hoverOnMouseOver,String hoverOnMouseOut) {
        this.hoverOnMouseOut=hoverOnMouseOut;
        this.hoverOnMouseOver=hoverOnMouseOver;
        // "return overlib('" + StringUtils.singleQuoteEncoder(text) + "');"
        // "return nd();"
    }

    public void enablePretty() {
        pretty=true;
    }

    public void disablePretty() {
        pretty=false;
    }


    public HTMLGenerator(PrintWriter pw) {
        this.pw = pw;
    }

    int counter = 0;

    public String id() {
        return "ID" + (counter++);
    }

    public class ElementAttributes {
        private ElementBase base;

        public ElementAttributes(ElementBase base) {
            this.base = base;
        }

        public ElementBase close() {
            pw.print(">");
            tagOpen = false;
            return base;
        }


        public ElementAttributes printAttr(String name, String value) {
            pw.print(" " + name + "=\"" + StringUtils.xmlEncoder(value, StringUtils.MASK_FULL_HTML) + "\"");
            return this;
        }

        public ElementAttributes printAttr(String name) {
            pw.print(" " + name);
            return this;
        }

        public ElementAttributes id(String id) {
            printAttr("id", id);
            return this;
        }

        public void closeEnd() {
            pw.print(" />");
            tagOpen = false;
            elements.pop();
            margin.delete(0, 1);
            if (pretty) {
                pw.println();
                pw.print(margin);
            }
        }


    }

    public class ElementBase {

        public ElementBase printText(String s) {
            pw.print(StringUtils.xmlEncoder(s, StringUtils.MASK_XML_TEXT));
            return this;
        }

        public ElementBase printEntity(String name) {
            pw.print("&"+name+";");
            return this;
        }

        public ElementBase nbsp() {
            return printEntity("nbsp");
        }

        public ElementAttributes attrs = new ElementAttributes(this);

        String name;


        public ElementBase(String name) {
            this.name = name;
        }

        public ElementBase start() {
            openTag().close();
            return this;
        }


        public ElementAttributes openTag() {
            if (pretty) {
                pw.println();
                pw.print(margin);
            }
            margin.append(" ");
            pw.print("<" + name);
            tagOpen = true;
            elements.push(name);
            return attrs;
        }

        public void end() {
            elements.pop();
            pw.print("</" + name + ">");
            margin.delete(0, 1);
            if (pretty) {
                pw.println();
                pw.print(margin);
            }
        }

        public void next() {
            end();
            start();
        }

        public void print(String text) {
            start().printText(text).end();
        }

        public void print() {
            openTag().closeEnd();
        }

    }

    public class HTMLElementBase extends ElementBase {
        public HTMLElementBase(String name) {
            super(name);
            attrs = new HTMLElementAttributes();
        }

        public HTMLElementAttributes openHTMLTag() {
            return (HTMLGenerator.HTMLElementBase.HTMLElementAttributes) super.openTag();
        }


        public class HTMLElementAttributes extends ElementAttributes {
            public HTMLElementAttributes() {
                super(HTMLElementBase.this);
            }

            public HTMLElementAttributes cssClass(String clazz) {
                printAttr("class", clazz);
                return this;
            }

            public HTMLElementAttributes cssStyle(String style) {
                printAttr("style", style);
                return this;
            }

            public HTMLElementAttributes hover(String text) {
                printAttr("onmouseover", StringUtils.findReplace(hoverOnMouseOver,"<>",StringUtils.singleQuoteEncoder(text)));
                printAttr("onmouseout", hoverOnMouseOut);
                return this;
            }

        }
    }

    public class HTMLElement extends HTMLElementBase {
        public HTMLElement(String name) {
            super(name);
        }

        public HTMLElementAttributes open() {
            return openHTMLTag();
        }
    }


    public class InputElement extends HTMLElement {
        private String type;

        public InputElement(String type) {
            super("input");
            this.type = type;
        }

        public HTMLElementAttributes open() {
            return (HTMLGenerator.HTMLElementBase.HTMLElementAttributes) super.openTag().printAttr("type", type);

        }

        public HTMLElementAttributes open(String name, String value) {
            return (HTMLElementAttributes) open().printAttr("name", name).printAttr("value", value);
        }

        public HTMLElementAttributes open(String name) {
            return (HTMLElementAttributes) open().printAttr("name", name);
        }

        public void print(String name, String value) {
            open().printAttr("name", name).printAttr("value", value).closeEnd();
        }

        public HTMLElementAttributes open(String name, String value,int size) {
            return (HTMLElementAttributes) open().printAttr("name", name).printAttr("value", value).printAttr("size", "" + size);
        }

    }

    public class CheckboxElement extends InputElement {

        public CheckboxElement() {
            super("checkbox");
        }

        public HTMLElementAttributes open(String name, boolean checked) {
            open().printAttr("name", name);
            if (checked) attrs.printAttr("checked");
            return (HTMLElementAttributes) attrs;
        }

    }

    public class TextAreaElement extends HTMLElement {

        public TextAreaElement() {
            super("textarea");
        }

        public ElementAttributes open(String name) {
            return openTag().printAttr("name", name);
        }

    }

    public class FormElement extends HTMLElement {

        public FormElement() {
            super("form");
        }

        public ElementAttributes open(String action, String method) {
            return openTag().printAttr("action", action).printAttr("method", method);
        }

    }

    public class ImageElement extends HTMLElement {

        public ImageElement() {
            super("img");
        }

        public HTMLElementAttributes open(String href) {
            return (HTMLElementAttributes) openTag().printAttr("src", href);
        }

    }



    public class LinkElement extends HTMLElement {

        StringBuffer location = new StringBuffer();
        boolean parameters = false;

        public LinkElement() {
            super("a");
            attrs = new LinkElementAttributes();
        }


        public LinkElementAttributes open(String location) {
            parameters = false;
            this.location.setLength(0);
            this.location.append(location);
            return (LinkElementAttributes) openTag();
        }


        public class LinkElementAttributes extends HTMLElementAttributes {


            public LinkElementAttributes() {
                super();
            }

            public LinkElementAttributes target(String target) {
                printAttr("target", target);
                return this;
            }

            public LinkElementAttributes addParameter(String name, String value) {
                if (parameters) location.append("&"); else location.append("?");
                location.append(name);
                location.append("=");
                location.append(URLUtils.encodeURL(value));
                parameters = true;
                return this;
            }

            public ElementBase close() {
                printAttr("href", location.toString());
                return super.close();
            }
        }
    }

    public class TableElement extends HTMLElement {

        public TableElement() {
            super("table");
        }


    }




    public class TDElement extends HTMLElementBase {

        public TDElement() {
            super("td");
            attrs = new TDElementAttributes();
        }
        

        public class TDElementAttributes extends HTMLElementAttributes {
            public TDElementAttributes colspan(int span) {
                return (HTMLGenerator.TDElement.TDElementAttributes) printAttr("colspan", "" + span);
            }
        }

        public TDElementAttributes open() {return (TDElementAttributes) openTag();}

        public TDElementAttributes open(int span) {return ((TDElementAttributes) openTag()).colspan(span);}

    }

    public class BaseElement extends HTMLElementBase {

        public BaseElement() {
            super("base");
            attrs = new BaseElementAttributes();
        }

        public class BaseElementAttributes extends HTMLElementAttributes {
            public BaseElementAttributes target(String target) {
                return (BaseElementAttributes) printAttr("target", target);
            }
        }

        public BaseElementAttributes open() {return (BaseElementAttributes) openTag();}


    }


    public class HeadElement extends HTMLElement {

        HTMLElement link = new HTMLElement("link");
        HTMLElement script = new HTMLElement("script");
        HTMLElement title = new HTMLElement("title");

        public HeadElement() {
            super("head");
        }

        public void stylesheet(String href) {
            link.openTag().printAttr("rel", "stylesheet").printAttr("href", href).closeEnd();
        }

        public void title(String title) {
            this.title.openTag().close().printText(title).end();
        }

        public void addScript(String source) {
            script.openTag().close().printText("\n").printText(source).printText("\n").end();
        }

        public void scriptlink(String href) {
            script.openTag().printAttr("type", "text/javascript").printAttr("src", href).close().end();
        }

        public void scriptlink(String href,String comment) {
            script.openTag().printAttr("type", "text/javascript").printAttr("src", href).close().printText("<!--" + comment + "-->").end();

        }
    }

    public void printStackTrace(Exception e) {
        e.printStackTrace(getFixedPrintWriter());
    }

    public void printPreFormatLine(String st) {
        code.start().printText(st).end();
        br.print();
    }


    public PrintWriter getFixedPrintWriter() {

        final String linesep = System.getProperty("line.separator");

        return new PrintWriter(new Writer() {

            StringBuffer lineAcc = new StringBuffer();

            public void close() throws IOException {}

            public void flush() throws IOException {}

            public void write(char cbuf[], int off, int len) throws IOException {
                String st = new String(cbuf, off, len);
                int c = 0;
                while (true) {
                    int p = st.indexOf(linesep, c);
                    if (p == -1) {
                        lineAcc.append(st.substring(c));
                        break;
                    }
                    lineAcc.append(st.substring(c, p));
                    printPreFormatLine(lineAcc.toString());
                    lineAcc.setLength(0);
                    c = p + linesep.length();
                }
            }
        });
    }

}

