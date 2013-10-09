package be.re.gui.util;

import be.re.io.ReaderWriterConnector;
import be.re.util.Mailcap;
import be.re.xml.ExpandedName;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;



/**
 * A simple HTML rendering pane.
 * @author Werner Donn\u00e9
 */

public class HTMLPane extends JEditorPane

{

  private final static StyleSheet	DEFAULT_STYLE_SHEET =
    new HTMLEditorKit().getStyleSheet();

  private Component	linkFrameReference;
  private JMenuBar	helpMenuBar;
  private JPopupMenu	popupMenu;
  private String	title;
  private Stack		toGoBackward = new Stack();
  private Stack		toGoForward = new Stack();



  public
  HTMLPane()
  {
    this((Component) null);
  }



  public
  HTMLPane(Component linkFrameReference)
  {
    this(null, linkFrameReference);
  }



  public
  HTMLPane(final URL document)
  {
    this(document, (Component) null);
  }



  public
  HTMLPane(final URL document, Component linkFrameReference)
  {
    this
    (
      document,
      HTMLPane.class.getResource("res/html_pane.css"),
      linkFrameReference
    );
  }



  public
  HTMLPane(final URL document, URL styleSheet)
  {
    this(document, styleSheet, null);
  }



  public
  HTMLPane(final URL document, URL styleSheet, Component linkFrameReference)
  {
    this(document, styleSheet, null, linkFrameReference);
  }



  public
  HTMLPane(final URL document, URL styleSheet, JMenuBar helpMenuBar)
  {
    this(document, styleSheet, helpMenuBar, null);
  }



  public
  HTMLPane
  (
    final URL	document,
    URL		styleSheet,
    JMenuBar	helpMenuBar,
    Component	linkFrameReference
  )
  {
    super();
    this.linkFrameReference = linkFrameReference;
    setEditorKit(createEditorKit(document, styleSheet));

    if (document != null)
    {
      setText(getDocument(document));
    }

    setEditable(false);
    setHelpMenuBar(helpMenuBar);

    addHyperlinkListener
    (
      new HyperlinkListener()
      {
        public void
        hyperlinkUpdate(HyperlinkEvent e)
        {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
          {
            String	href = e.getDescription();

            if (href.indexOf('#') == 0)
            {
              if
              (
                HTMLPane.this.helpMenuBar != null && href.startsWith("#action:")
              )
              {
                showMenu(e.getDescription().substring("#action:".length()));
              }
              else
              {
                toGoBackward.push(getVisibleRect());
                scrollToReference(href.substring(1));
                toGoForward.clear();
              }
            }
            else
            {
              try
              {
                if
                (
                  href.startsWith("http://")	||
                  href.startsWith("https://")	||
                  href.startsWith("ftp://")	||
                  href.startsWith("file:/")
                )
                {
                  Mailcap.exec(new URL(href));
                }
                else
                {
                  Util.showHtml
                  (
                    e.getDescription().startsWith("resource:/") ?
                      HTMLPane.class.getResource
                      (
                        e.getDescription().substring("resource:".length())
                      ) : new URL(document, e.getDescription()),
                    HTMLPane.this.helpMenuBar,
                    Util.getTop(getParent())
                  );
                }
              }

              catch (Exception ex)
              {
                throw new RuntimeException(ex);
              }
            }
          }
        }
      }
    );

    setKeyboardActions();
  }



  public void
  addNotify()
  {
    super.addNotify();
    setCaretPosition(0);
    Util.noBorders(this);
  }



  private void
  changeMenu(List menu, boolean open)
  {
    if (popupMenu != null)
    {
      Point	point = getVisibleRect().getLocation();

      SwingUtilities.convertPointToScreen(point, this);
      popupMenu.setLocation(point);
      popupMenu.setVisible(open);
    }

    for (Iterator i = menu.iterator(); i.hasNext();)
    {
      JMenuItem	item = (JMenuItem) i.next();

      if (item instanceof JMenu)
      {
        ((JMenu) item).setPopupMenuVisible(open);
        item.setSelected(open);
      }
      else
      {
        item.setArmed(open);
      }
    }
  }



  private static JMenu
  copyMenu(JMenu menu)
  {
    JMenu	result = new JMenu(menu.getText());

    for (int i = 0; i < menu.getItemCount(); ++i)
    {
      if (menu.getItem(i) != null)
      {
        result.add
        (
          menu.getItem(i) instanceof JMenu ?
            copyMenu((JMenu) menu.getItem(i)) : copyMenuItem(menu.getItem(i))
        );
      }
    }

    return result;
  }



  private static JMenuBar
  copyMenuBar(JMenuBar menuBar)
  {
    JMenuBar	result = new JMenuBar();

    for (int i = 0; i < menuBar.getMenuCount(); ++i)
    {
      result.add(copyMenu(menuBar.getMenu(i)));
    }

    return result;
  }



  private static JMenuItem
  copyMenuItem(JMenuItem item)
  {
    JMenuItem	result = new JMenuItem(item.getText(), item.getMnemonic());

    result.setAccelerator(item.getAccelerator());
    result.setActionCommand(item.getActionCommand());

    return result;
  }



  private static HTMLEditorKit
  createEditorKit(final URL document, URL styleSheetUrl)
  {
    HTMLEditorKit	kit =
      new HTMLEditorKit()
      {
        public javax.swing.text.Document
        createDefaultDocument()
        {
          HTMLDocument	doc = (HTMLDocument) super.createDefaultDocument();

          doc.setAsynchronousLoadPriority(-1);

          if (document != null)
          {
            doc.setBase(document);
          }

          return doc;
        }
      };

    if (styleSheetUrl != null)
    {
      StyleSheet	styleSheet = new StyleSheet();

      try
      {
        styleSheet.loadRules
        (
          new InputStreamReader(styleSheetUrl.openStream()),
          null
        );

        styleSheet.addStyleSheet(DEFAULT_STYLE_SHEET);
        kit.setStyleSheet(styleSheet);
      }

      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }

    return kit;
  }



  private static JPopupMenu
  createPopupMenu(JMenuBar menuBar)
  {
    JPopupMenu	result = new JPopupMenu();

    for (int i = 0; i < menuBar.getMenuCount(); ++i)
    {
      result.add(copyMenu(menuBar.getMenu(i)));
    }

    return result;
  }



  private static boolean
  findMenu(JMenuItem item, String action, List chain)
  {
    if (item == null)
    {
      return false;
    }

    if (action.equals(item.getActionCommand()))
    {
      chain.add(0, item);

      return true;
    }

    if (item instanceof JMenu)
    {
      for (int i = 0; i < ((JMenu) item).getItemCount(); ++i)
      {
        if (findMenu(((JMenu) item).getItem(i), action, chain))
        {
          chain.add(0, item);

          return true;
        }
      }
    }

    return false;
  }



  public static Rectangle
  getDefaultBounds(Dimension size, Component linkFrameReference)
  {
    if (size == null)
    {
      size = new Dimension(400, 600);
    }

    return
      linkFrameReference != null ?
        new Rectangle
        (
          new Point
          (
            (int)
              (
                linkFrameReference.getX() > size.getWidth() + 20 ?
                  (linkFrameReference.getX() - size.getWidth() - 20) :
                  (
                    linkFrameReference.getX() + linkFrameReference.getWidth() <
                      Toolkit.getDefaultToolkit().getScreenSize().getWidth() -
                        size.getWidth() - 20 ?
                      (
                        linkFrameReference.getX() +
                          linkFrameReference.getWidth() + 20
                      ) : 0
                  )
              ),
            linkFrameReference.getY()
          ),
          size
        ) :
        new Rectangle
        (
          new Point
          (
            (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2) -
              (int) (size.getWidth() / 2),
            (int)
              (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2) -
              (int) (size.getHeight() / 2)
          ),
          size
        );
  }



  private static String
  getDocument(URL document)
  {
    try
    {
      StringWriter	writer = new StringWriter();

      ReaderWriterConnector.copy
      (
        new InputStreamReader(document.openStream()),
        writer
      );

      return writer.toString();
    }

    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }



  public JMenuBar
  getHelpMenuBar()
  {
    return helpMenuBar;
  }



  public String
  getTitle()
  {
    return title;
  }



  private static String
  getTitle(Document document)
  {
    try
    {
      return
        be.re.xml.Util.getText
        (
          be.re.xml.Util.selectElement
          (
            document.getDocumentElement(),
            new ExpandedName[]
            {new ExpandedName(null, "head"), new ExpandedName(null, "title")}
          )
        );
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private static void
  removeIndexElements(Node node)
  {
    if (node != null)
    {
      Node	sibling = node.getNextSibling();

      if
      (
        "span".equals(node.getLocalName())			&&
        "index".equals(((Element) node).getAttribute("class"))
      )
      {
        node.getParentNode().removeChild(node);
      }
      else
      {
        removeIndexElements(node.getFirstChild());
      }

      removeIndexElements(sibling);
    }
  }



  public void
  setHelpMenuBar(JMenuBar helpMenuBar)
  {
    this.helpMenuBar = helpMenuBar;

    if (helpMenuBar != null && be.re.util.Util.isMac())
    {
      popupMenu = createPopupMenu(helpMenuBar);
    }
    else
    {
      popupMenu = null;
    }
  }



  private void
  setKeyboardActions()
  {
    getInputMap().
      put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, Event.CTRL_MASK), "home");

    getActionMap().put
    (
      "home",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          scrollRectToVisible(new Rectangle(0, 0, 0, 0));
        }
      }
    );

    getInputMap().
      put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK), "back");

    getActionMap().put
    (
      "back",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          if (!toGoBackward.empty())
          {
            Rectangle	rect = (Rectangle) toGoBackward.pop();

            toGoForward.push(getVisibleRect());
            scrollRectToVisible(rect);
          }
        }
      }
    );

    getInputMap().
      put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.ALT_MASK), "forward");

    getActionMap().put
    (
      "forward",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          if (!toGoForward.empty())
          {
            Rectangle	rect = (Rectangle) toGoForward.pop();

            toGoBackward.push(getVisibleRect());
            scrollRectToVisible(rect);
          }
        }
      }
    );

    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");

    getActionMap().put
    (
      "down",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          Rectangle	rect = getVisibleRect();

          rect.setLocation
          (
            (int) rect.getX(),
            (int) rect.getY() +
              getScrollableUnitIncrement(rect, SwingConstants.VERTICAL, 1)
          );

          scrollRectToVisible(rect);
        }
      }
    );

    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");

    getActionMap().put
    (
      "up",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          Rectangle	rect = getVisibleRect();

          rect.setLocation
          (
            (int) rect.getX(),
            (int) rect.getY() -
              getScrollableUnitIncrement(rect, SwingConstants.VERTICAL, -1)
          );

          scrollRectToVisible(rect);
        }
      }
    );
  }



  public void
  setText(String text)
  {
    try
    {
      Document		document =
        be.re.xml.Util.newDocumentBuilderFactory(false).newDocumentBuilder().
          parse(new InputSource(new StringReader(text)));
      Transformer	transformer =
        TransformerFactory.newInstance().newTransformer();
      StringWriter	writer = new StringWriter();

      removeIndexElements(document.getDocumentElement());
      title = getTitle(document);
      transformer.setOutputProperty("method", "xml");
      transformer.setOutputProperty("omit-xml-declaration", "yes");
      transformer.transform(new DOMSource(document), new StreamResult(writer));
      super.setText(writer.toString());
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private static void
  sleep(long time)
  {
    try
    {
      Thread.sleep(time);
    }

    catch (InterruptedException e)
    {
    }
  }



  private void
  showMenu(String action)
  {
    Container	search =
      popupMenu != null ? (Container) popupMenu : (Container) helpMenuBar;

    for (int i = 0; i < search.getComponentCount(); ++i)
    {
      final List	menu = new ArrayList();

      if (findMenu((JMenuItem) search.getComponent(i), action, menu))
      {
        changeMenu(menu, true);

        new Thread
        (
          new Runnable()
          {
            public void
            run()
            {
              sleep(3000);
              changeMenu(menu, false);
            }
          }
        ).start();

        return;
      }
    }
  }

} // HTMLPane
