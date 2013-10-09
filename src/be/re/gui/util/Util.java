package be.re.gui.util;

import be.re.gui.form.SimpleFormDialog;
import be.re.net.BasicUser;
import be.re.net.User;
import be.re.xml.ExpandedName;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;



/**
 * @author Werner Donn\u00e9
 */

public class Util

{

  private static ResourceBundle	bundle = null;



  private static void
  addComponentListener(final JComponent component)
  {
    addComponentListener
    (
      component.getComponents(),
      new ComponentListener()
      {
        public void
        componentHidden(ComponentEvent e)
        {
          component.repaint();
        }

        public void
        componentMoved(ComponentEvent e)
        {
          component.repaint();
        }

        public void
        componentResized(ComponentEvent e)
        {
          component.repaint();
        }

        public void
        componentShown(ComponentEvent e)
        {
          component.repaint();
        }
      }
    );
  }



  private static void
  addComponentListener(Component[] components, ComponentListener l)
  {
    for (int i = 0; i < components.length; ++i)
    {
      components[i].addComponentListener(l);

      if (components[i] instanceof Container)
      {
        addComponentListener(((Container) components[i]).getComponents(), l);
      }
    }
  }



  public static JPanel
  addPadding
  (
    final JComponent	component,
    final int		top,
    final int		left,
    final int		bottom,
    final int		right
  )
  {
    JPanel	result =
      new JPanel(new GridBagLayout())
      {
        public Dimension
        getPreferredSize()
        {
          return
            new Dimension
            (
              (int) component.getPreferredSize().getWidth() + left + right,
              (int) component.getPreferredSize().getHeight() + top + bottom
            );
        }

        public Dimension
        getMaximumSize()
        {
          return getPreferredSize();
        }
      };

    result.add
    (
      component,
      new GridBagConstraints
      (
        0,
        0,
        1,
        1,
        1.0,
        1.0,
        GridBagConstraints.CENTER,
        GridBagConstraints.NONE,
        new Insets(top, left, bottom, right),
        0,
        0
      )
    );

    result.setOpaque(false);

    return result;
  }



  public static void
  adjustColumns(JTable table)
  {
    for (int i = 0; i < table.getColumnCount(); ++i)
    {
      table.getColumnModel().getColumn(i).
        setPreferredWidth
        (
          getWidestCell
          (
            table,
            table.convertColumnIndexToModel(i),
            table.getColumnModel().getColumn(i).getHeaderValue().toString()
          )
        );
    }
  }



  public static char[]
  askPassword(String title)
  {
    User			user = new BasicUser();
    final AuthenticateDialog	dialog =
      new AuthenticateDialog(title, user, true);

    SwingUtilities.invokeLater
    (
      new Runnable()
      {
        public void
        run()
        {
          dialog.setVisible(true);
        }
      }
    );

    synchronized (dialog)
    {
      try
      {
        dialog.wait();
      }

      catch (InterruptedException e)
      {
        throw new RuntimeException(e);
      }
    }

    return user.getPassword() == null ? null : user.getPassword().toCharArray();
  }



  public static void
  center(Component component)
  {
    component.setLocation
    (
      (int) (component.getToolkit().getScreenSize().getWidth() / 2) -
        (int) (component.getSize().getWidth() / 2),
      (int) (component.getToolkit().getScreenSize().getHeight() / 2) -
        (int) (component.getSize().getHeight() / 2)
    );
  }



  public static JButton
  createIconButton(Icon icon)
  {
    return createIconButton(icon, false);
  }



  public static JButton
  createIconButton(Icon icon, boolean paintBorder)
  {
    final JButton   result = icon != null ? new JButton(icon) : new JButton();

    result.setContentAreaFilled(false);
    result.setBorderPainted(paintBorder);
    result.setFocusPainted(false);
    result.setFocusable(false);

    result.setUI
    (
      new BasicButtonUI()
      {
        public Dimension
        getPreferredSize(JComponent c)
        {
          return
            result.getIcon() != null ?
              new Dimension
              (
                result.getIcon().getIconWidth() + 2,
                result.getIcon().getIconHeight() + 2
              ) : super.getPreferredSize(c);
        }
      }
    );

    return result;
  }



  public static JComponent
  createTableControls(JTable table, int leftMargin)
  {
    JPanel	panel = new JPanel();

    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.add(Box.createRigidArea(new Dimension(leftMargin, 0)));
    panel.add(new TableControls(table));
    panel.add(Box.createHorizontalGlue());

    return panel;
  }



  public static java.awt.Frame
  getFrame(Component component)
  {
    return
      component == null ?
        null :
        (
          component instanceof java.awt.Frame ?
            (java.awt.Frame) component : getFrame(component.getParent())
        );
  }



  public static Window
  getOwner(Component component)
  {
    return
      component == null ?
        null :
        (
          component instanceof Window &&
            ((Window) component).getOwner() != null ?
            ((Window) component).getOwner() :
            getOwner(component.getParent())
        );
  }



  public static String
  getResource(String name)
  {
    if (bundle == null)
    {
      bundle = ResourceBundle.getBundle("be.re.gui.util.res.Res");
    }

    return bundle.getString(name);
  }



  public static Component
  getTop(Container container)
  {
    return
      container == null ?
        null :
        (
          container instanceof Dialog || container instanceof java.awt.Frame ?
            container : getTop(container.getParent())
        );
  }



  private static int
  getWidestCell(JTable table, int column, String title)
  {
    int		length = title.length();
    String	longest = title;

    for (int i = 0; i < table.getRowCount(); ++i)
    {
      String	s =
        table.getValueAt(i, column) != null ?
          table.getValueAt(i, column).toString() : "";
      int	len = s.length();

      if (len > length)
      {
        length = len;
        longest = s;
      }
    }

    return
      length == 0 && table.getColumnModel() != null &&
        table.getColumnModel().getColumn(column) != null ?
        table.getColumnModel().getColumn(column).getPreferredWidth() :
        (
          table.getGraphics() != null &&
            table.getGraphics().getFontMetrics() != null ?
            table.getGraphics().getFontMetrics().stringWidth(longest + "MMM") :
              // Safety margin added.
            length
        );
  }



  public static boolean
  isRowBlockSelected(JTable table)
  {
    int[]	rows = table.getSelectedRows();

    if (rows.length == 0)
    {
      return false;
    }

    Arrays.sort(rows);

    for (int i = 0; i < rows.length; ++i)
    {
      if (i > 0 && rows[i] > rows[i - 1] + 1)
      {
        return false;
      }
    }

    return true;
  }



  public static JComponent
  mirror(final JComponent component, final double percentHeight)
  {
    try
    {
      final VolatileImage[]	image = new VolatileImage[1];
      final JPanel		panel = new JPanel();
      final JPanel		mirrorPanel =
        new JPanel()
        {
          public Dimension
          getPreferredSize()
          {
            return
              new Dimension
              (
                (int) component.getPreferredSize().getWidth(),
                (int) (percentHeight * component.getPreferredSize().getHeight())
              );
          }

          public void
          paint(Graphics g)
          {
            if (image[0] != null)
            {
              Graphics2D	g2d = (Graphics2D) g;
              AffineTransform	saved = g2d.getTransform();

              g2d.translate(0, image[0].getHeight());
              g2d.scale(1, -1);
              g2d.drawImage(image[0], null, null);
              g2d.setTransform(saved);

              g2d.setPaint
              (
                new java.awt.GradientPaint
                (
                  0,
                  0,
                  new Color(0, 0, 0, 128),
                  0,
                  getHeight(),
                  new Color(0, 0, 0, 255)
                )
              );

              g2d.fillRect(0, 0, getWidth(), getHeight());
            }
          }
        };
      JPanel			componentPanel =
        new JPanel(new BorderLayout())
        {
          public void
          paint(Graphics g)
          {
            image[0] = createVolatileImage(getWidth(), getHeight());

            Graphics	gi = image[0].createGraphics();

            super.paint(gi);
            gi.dispose();
            super.paint(g);
            mirrorPanel.repaint();
          }
        };

      addComponentListener(mirrorPanel);
      componentPanel.add(component, BorderLayout.CENTER);
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.add(componentPanel);
      panel.add(mirrorPanel);
      panel.setOpaque(false);
      componentPanel.setOpaque(false);

      return panel;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static void
  noBorders(JComponent component)
  {
    try
    {
      component.setBorder(BorderFactory.createEmptyBorder());
    }

    catch (Exception e)
    {
      // May not be supported.
    }

    if
    (
      component.getParent() != null			&&
      component.getParent() instanceof JComponent
    )
    {
      noBorders((JComponent) component.getParent());
    }
  }



  public static void
  removeSelectedRows(JTable table)
  {
    if (table.getModel() instanceof DefaultTableModel)
    {
      int[]	rows = table.getSelectedRows();

      Arrays.sort(rows);

      for (int i = rows.length - 1; i >= 0; --i)
      {
        ((DefaultTableModel) table.getModel()).removeRow(rows[i]);
      }
    }
  }



  public static void
  report(String message)
  {
    PrintWriter	writer =
      new PrintWriter
      (
        InteractiveWriter.open(Util.getResource("title_execution_report"))
      );

    writer.println(message);
    writer.close();
  }



  /**
   * Sets the background recursively.
   */

  public static void
  setBackground(Container container, Color color)
  {
    container.setBackground(color);

    Component[]	components = container.getComponents();

    for (int i = 0; i < components.length; ++i)
    {
      if (components[i] instanceof Container)
      {
        setBackground((Container) components[i], color);
      }
      else
      {
        components[i].setBackground(color);
      }
    }
  }



  public static void
  setDebugBorders(JComponent component)
  {
    try
    {
      component.setBorder(BorderFactory.createLineBorder(Color.RED));
    }

    catch (Exception e)
    {
      // May not be supported.
    }

    if
    (
      component.getParent() != null			&&
      component.getParent() instanceof JComponent
    )
    {
      setDebugBorders((JComponent) component.getParent());
    }
  }



  public static void
  setMnemonic(JButton button, String label)
  {
    setMnemonic((Object) button, label);
  }



  public static void
  setMnemonic(JMenuItem item, String label)
  {
    setMnemonic((Object) item, label);
  }



  public static void
  setMnemonic(Object object, String label)
  {
    int	i;

    for
    (
      i = label.indexOf('&');
      i > 0 && label.charAt(i - 1) == '\\';
      i = label.indexOf('&', i + 1)
    );

    if (i != -1)
    {
      char	c = Character.toUpperCase(label.charAt(i + 1));

      if
      (
        (
          c >= KeyEvent.VK_0	&&
          c <= KeyEvent.VK_9
        )			||
        (
          c >= KeyEvent.VK_A	&&
          c <= KeyEvent.VK_Z
        )
      )
      {
        try
        {
          object.getClass().
            getMethod("setMnemonic", new Class[]{int.class}).
            invoke(object, new Object[]{new Integer(c)});

          object.getClass().
            getMethod("setDisplayedMnemonicIndex", new Class[]{int.class}).
            invoke
            (
              object,
              new Object[]
              {
                new Integer
                (
                  unescapeMnemonic(label.substring(0, i + 1)).length()
                )
              }
            );
        }

        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }



  public static void
  setStyle(JTable table)
  {
    table.setIntercellSpacing(new Dimension(0, 0));
    table.setShowGrid(false);
    table.setShowHorizontalLines(false);
    table.setShowVerticalLines(false);
  }



  public static void
  showAbout(URL document, URL stylesheet)
  {
    final JFrame	frame = new JFrame(getResource("title_about"));
    final HTMLPane	pane = new HTMLPane(document, stylesheet);

    if (Frame.getDefaultIcon() != null)
    {
      frame.setIconImage(Frame.getDefaultIcon().getImage());
    }

    frame.getContentPane().add(pane);
    frame.pack();

    frame.setLocation
    (
      (int) (frame.getToolkit().getScreenSize().getWidth() / 2) -
        (int) (frame.getSize().getWidth() / 2),
      (int) (frame.getToolkit().getScreenSize().getHeight() / 2) -
        (int) (frame.getSize().getHeight() / 2)
    );

    pane.addMouseListener
    (
      new MouseAdapter()
      {
        public void
        mouseClicked(MouseEvent e)
        {
          frame.dispose();
        }
      }
    );

    pane.getInputMap().
      put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

    pane.getActionMap().put
    (
      "close",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          frame.dispose();
        }
      }
    );

    SwingUtilities.invokeLater
    (
      new Runnable()
      {
        public void
        run()
        {
          frame.setVisible(true);
        }
      }
    );
  }



  public static void
  showFields(URL resource, Properties fields, ResourceBundle bundle)
  {
    showFields(resource, fields, bundle, null, null);
  }



  public static void
  showFields
  (
    URL			resource,
    Properties		fields,
    ResourceBundle	bundle,
    Component		linkFrameReference
  )
  {
    showFields(resource, fields, bundle, null, linkFrameReference);
  }



  public static void
  showFields
  (
    URL			resource,
    Properties		fields,
    ResourceBundle	bundle,
    Dimension		bounds
  )
  {
    showFields(resource, fields, bundle, bounds, null);
  }



  public static void
  showFields
  (
    URL			resource,
    Properties		fields,
    ResourceBundle	bundle,
    Dimension		bounds,
    Component		linkFrameReference
  )
  {
    showHtml
    (
      null,
      transform(resource, fields, bundle),
      bounds,
      null,
      linkFrameReference
    );
  }



  public static void
  showHtml(URL resource)
  {
    showHtml(resource, null, null, null);
  }



  public static void
  showHtml(URL resource, Component linkFrameReference)
  {
    showHtml(resource, null, null, linkFrameReference);
  }



  public static void
  showHtml(URL resource, JMenuBar helpMenuBar)
  {
    showHtml(resource, null, helpMenuBar, null);
  }



  public static void
  showHtml(URL resource, JMenuBar helpMenuBar, Component linkFrameReference)
  {
    showHtml(resource, null, helpMenuBar, linkFrameReference);
  }



  public static void
  showHtml(URL resource, Dimension bounds)
  {
    showHtml(resource, bounds, null, null);
  }



  public static void
  showHtml(URL resource, Dimension bounds, JMenuBar helpMenuBar)
  {
    showHtml(resource, bounds, helpMenuBar, null);
  }



  public static void
  showHtml
  (
    URL		resource,
    Dimension	bounds,
    JMenuBar	helpMenuBar,
    Component	linkFrameReference
  )
  {
    showHtml(resource, null, bounds, helpMenuBar, linkFrameReference);
  }



  public static void
  showHtml(String text)
  {
    showHtml(text, null, null, null);
  }



  public static void
  showHtml(String text, Component linkFrameReference)
  {
    showHtml(text, null, null, linkFrameReference);
  }



  public static void
  showHtml(String text, JMenuBar helpMenuBar)
  {
    showHtml(text, null, helpMenuBar, null);
  }



  public static void
  showHtml(String text, JMenuBar helpMenuBar, Component linkFrameReference)
  {
    showHtml(text, null, helpMenuBar, linkFrameReference);
  }



  public static void
  showHtml(String text, Dimension bounds)
  {
    showHtml(text, bounds, null, null);
  }



  public static void
  showHtml(String text, Dimension bounds, JMenuBar helpMenuBar)
  {
    showHtml(text, bounds, helpMenuBar, null);
  }



  public static void
  showHtml
  (
    String	text,
    Dimension	bounds,
    JMenuBar	helpMenuBar,
    Component	linkFrameReference
  )
  {
    showHtml(null, text, bounds, helpMenuBar, linkFrameReference);
  }



  private static void
  showHtml
  (
    URL		resource,
    String	text,
    Dimension	bounds,
    JMenuBar	helpMenuBar,
    Component	linkFrameReference
  )
  {
    HTMLPane	pane =
      new HTMLPane
      (
        resource,
        HelpButton.class.getResource(Util.getResource("help_style_sheet")),
        helpMenuBar,
        linkFrameReference
      );

    if (resource == null && text != null)
    {
      pane.setText(text);
    }

    final Frame	frame =
      new Frame
      (
        pane.getTitle() != null ?
          pane.getTitle() : Util.getResource("title_help"),
        false,
        HTMLPane.getDefaultBounds(bounds, linkFrameReference),
        false
      );

    frame.getContentPane().add
    (
      new JScrollPane
      (
        pane,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
      )
    );

    frame.getRootPane().
      getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put
      (
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        "close"
      );

    frame.getRootPane().getActionMap().put
    (
      "close",
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          frame.dispose();
        }
      }
    );

    SwingUtilities.invokeLater
    (
      new Runnable()
      {
        public void
        run()
        {
          frame.setVisible(true);
        }
      }
    );
  }



  public static void
  showHtmlMessage(URL document, URL stylesheet)
  {
    showMessage(new HTMLPane(document, stylesheet));
  }



  public static void
  showMessage(String s)
  {
    showMessage(s, false);
  }



  public static void
  showMessage(String s, boolean multiline)
  {
    showMessage(s, multiline, null);
  }



  public static void
  showMessage(String s, boolean multiline, Runnable done)
  {
    try
    {
      Document	form =
        be.re.xml.Util.getDocumentBuilder(null, false).parse
        (
          Util.class.getResource
          (
            multiline ?
              "forms/message_box_multiline.xml" : "forms/message_box.xml"
          ).toString()
        );

      be.re.xml.Util.selectElement
      (
        form.getDocumentElement(),
        new ExpandedName[]
        {
          new ExpandedName("http://www.w3.org/1999/xhtml", "div"),
          new ExpandedName
          (
            "http://www.w3.org/1999/xhtml",
            multiline ? "textarea" : "label"
          )
        }
      ).appendChild(form.createTextNode(s));

      SimpleFormDialog	dialog = new SimpleFormDialog(null, form, true, null);

      if (done == null)
      {
        dialog.setModal(true);
      }

      dialog.getFields();

      if (done != null)
      {
        done.run();
      }
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static void
  showMessage(JComponent component)
  {
    MessageDialog	dialog = new MessageDialog(null, component);

    dialog.setModal(true);
    dialog.open();
  }



  private static String
  transform(URL resource, Properties fields, ResourceBundle bundle)
  {
    try
    {
      Document		document =
        be.re.xml.Util.newDocumentBuilderFactory(false).newDocumentBuilder().
          parse(new InputSource(resource.toString()));
      Transformer	transformer =
        TransformerFactory.newInstance().newTransformer();
      StringWriter	writer = new StringWriter();

      document.normalizeDocument();
      transform(document.getDocumentElement().getFirstChild(), fields, bundle);
      transformer.setOutputProperty("method", "xml");
      transformer.setOutputProperty("omit-xml-declaration", "yes");
      transformer.transform(new DOMSource(document), new StreamResult(writer));

      return writer.toString();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private static void
  transform(Node node, Properties fields, ResourceBundle bundle)
  {
    if (node != null)
    {
      if (node instanceof Text)
      {
        ((Text) node).
          setData(transform(((Text) node).getData(), fields, bundle));
      }

      transform(node.getFirstChild(), fields, bundle);
      transform(node.getNextSibling(), fields, bundle);
    }
  }



  private static String
  transform(String s, Properties fields, ResourceBundle bundle)
  {
    int		position = 0;
    String	result = "";

    for (int i = 1; i < s.length(); ++i)
    {
      if (s.charAt(i - 1) == '$' && i < s.length() - 1)
      {
        int	index =
          s.charAt(i) == '[' ?
            s.indexOf(']', i + 1) :
            (s.charAt(i) == '{' ? s.indexOf('}', i + 1) : -1);

        if (index != -1)
        {
          result += s.substring(position, i - 1);
          position = index + 1;

          if (index > i + 1)
          {
            String	name = s.substring(i + 1, index);
            String	value =
              s.charAt(i) == '[' ?
                bundle.getString(name) : fields.getProperty(name);

            if (value != null)
            {
              result += value;
            }
          }

          i = index;
        }
      }
    }

    result += s.substring(position);

    return result.equals("") ? " " : result;
      // Empty table cells display a ">", but why?
  }



  public static String
  unescapeMnemonic(String label)
  {
    return
      label.replaceAll("^&", "").replaceAll("([^\\\\])&", "$1").
        replaceAll("\\\\", "");
  }

} // Util
