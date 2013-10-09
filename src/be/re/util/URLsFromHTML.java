package be.re.util;

import be.re.net.URLManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;



public class URLsFromHTML

{

  private static URL
  deriveAbsoluteURL(String url, URL base) throws MalformedURLException
  {
    if (base == null)
    {
      throw
        new MalformedURLException
        (
          "No relative URLs allowed without a base URL"
        );
    }

    return
      new URL
      (
        url.length() > 0 && url.charAt(0) == '/' ?
          (base.getProtocol() + "://" + base.getAuthority() + url) :
          (base.toString() + url)
      );
  }



  public static URL[]
  extract(InputStream input, URL base)
    throws IOException, MalformedURLException
  {
    CollectURLs	collector = new CollectURLs();

    new ParserDelegator().parse(new InputStreamReader(input), collector, true);

    Iterator	iterator = collector.getSet().iterator();
    URL		realBase =
      collector.getBase() != null ? new URL(collector.getBase()) : base;
    URL[]	result = new URL[collector.getSet().size()];

    if (realBase != null && !new URLManager().isContainer(realBase))
    {
      realBase = new URLManager().getParent(realBase);
    }

    for (int i = 0; i < result.length; ++i)
    {
      String	url = (String) iterator.next();

      result[i] =
        isRelative(url) ? deriveAbsoluteURL(url, realBase) : new URL(url);
    }

    return result;
  }



  private static boolean
  isRelative(String url)
  {
    try
    {
      new URL(url);

      return false;
    }

    catch (MalformedURLException e)
    {
      return true;
    }
  }



  public static class CollectURLs extends ParserCallback

  {

    private String	base = null;
    private HashSet	set = new HashSet();

    private Tuple[]	toBeHandled =
      {
        new Tuple(HTML.Tag.A, HTML.Attribute.HREF),
        new Tuple(HTML.Tag.APPLET, HTML.Attribute.CODEBASE),
        new Tuple(HTML.Tag.AREA, HTML.Attribute.HREF),
        new Tuple(HTML.Tag.BODY, HTML.Attribute.BACKGROUND), // deprecated
        //new Tuple(HTML.Tag.BLOCKQUOTE, HTML.Attribute.CITE), HTML4.0
        //new Tuple(HTML.Tag.DEL, HTML.Attribute.CITE), HTML4.0
        //new Tuple(HTML.Tag.FRAME, HTML.Attribute.LONGDESC), HTML4.0
        new Tuple(HTML.Tag.FRAME, HTML.Attribute.SRC),
        //new Tuple(HTML.Tag.IFRAME, HTML.Attribute.LONGDESC), HTML4.0
        //new Tuple(HTML.Tag.IFRAME, HTML.Attribute.SRC), HTML4.0
        //new Tuple(HTML.Tag.IMG, HTML.Attribute.LONGDESC), HTML4.0
        //new Tuple(HTML.Tag.IMG, HTML.Attribute.SRC),
        new Tuple(HTML.Tag.INPUT, HTML.Attribute.SRC),
        //new Tuple(HTML.Tag.INS, HTML.Attribute.CITE), HTML4.0
        new Tuple(HTML.Tag.LINK, HTML.Attribute.HREF),
        new Tuple(HTML.Tag.OBJECT, HTML.Attribute.ARCHIVE),
        new Tuple(HTML.Tag.OBJECT, HTML.Attribute.CLASSID),
        new Tuple(HTML.Tag.OBJECT, HTML.Attribute.CODEBASE),
        new Tuple(HTML.Tag.OBJECT, HTML.Attribute.DATA),
        new Tuple(HTML.Tag.SCRIPT, HTML.Attribute.SRC),
      };



    private
    CollectURLs()
    {
    }



    private void
    addURL(HTML.Tag t, MutableAttributeSet a)
    {
      if (t.equals(HTML.Tag.BASE))
      {
        if (a.getAttribute(HTML.Attribute.HREF) != null)
        {
          base = a.getAttribute(HTML.Attribute.HREF).toString();
        }
      }
      else
      {
        for (int i = 0; i < toBeHandled.length; ++i)
        {
          if (t.equals(toBeHandled[i].tag))
          {
            if (a.getAttribute(toBeHandled[i].attribute) != null)
            {
              set.add(a.getAttribute(toBeHandled[i].attribute).toString());
            }

            break;
          }
        }
      }
    }



    public void
    flush() throws BadLocationException
    {
    }



    private String
    getBase()
    {
      return base;
    }



    private HashSet
    getSet()
    {
      return set;
    }



    public void
    handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos)
    {
      addURL(t, a);
    }



    public void
    handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
    {
      addURL(t, a);
    }



    private static class Tuple

    {

      private HTML.Attribute	attribute;
      private HTML.Tag		tag;



      private
      Tuple(HTML.Tag tag, HTML.Attribute attribute)
      {
        this.tag = tag;
        this.attribute = attribute;
      }

    } // Tuple

  } // CollectURLs

} // URLsFromHTML
