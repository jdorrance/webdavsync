package be.re.gui.form;

import be.re.gui.util.LongList;
import be.re.gui.util.MimeTypeListCellRenderer;
import be.re.util.Array;
import be.re.util.EventMulticaster;
import be.re.util.MimeType;
import com.lavantech.gui.comp.DateTimePicker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * A simple form generator that recognizes some XHTML form elements. It can be
 * used to create simple dialog boxes for retrieving some fields from the user.
 * @author Werner Donn\u00e9
 */

public class SimpleForm extends JPanel

{

  private final static String[]	INLINE =
    new String[]{"input", "label", "select", "span", "textarea"};
  private final static String	XHTML = "http://www.w3.org/1999/xhtml";

  private ResourceBundle		bundle;
  private EventMulticaster		changeListeners =
    new EventMulticaster();
  private Map<String,Object>		controls = new HashMap<String,Object>();
  private static boolean		debug =
    System.getProperty("be.re.form.debug") != null;
  private Map<String,Map>		fields = new HashMap<String,Map>();
  private Map<String,ButtonGroup>	groups =
    new HashMap<String,ButtonGroup>();



  public
  SimpleForm(ResourceBundle bundle)
  {
    this(null, bundle);
  }



  /**
   * @param form the XML document containing the <q>form</q> document element.
   * @param bundle the resource bundle with which the <code>id</code>
   * attributes of <code>label</code> elements are resolved.
   */

  public
  SimpleForm(Document form, ResourceBundle bundle)
  {
    super(new GridBagLayout());
    this.bundle = bundle;
    setDocument(form);
  }



  public void
  addChangeListener(ChangeListener listener)
  {
    changeListeners.add(listener);
  }



  /**
   * The optgroup element is not supported.
   */

  private static void
  addOptions
  (
    Node		node,
    ResourceBundle	bundle,
    AddOption		add,
    SelectOption	select
  )
  {
    Element[]	children = be.re.xml.Util.selectElements(node);

    for (int i = 0; i < children.length; ++i)
    {
      if ("option".equals(children[i].getLocalName()))
      {
        Option	option =
          new Option
          (
            !"".equals(((Element) children[i]).getAttribute("value")) ?
              ((Element) children[i]).getAttribute("value") :
              be.re.xml.Util.getText(children[i]),
            !"".equals(((Element) children[i]).getAttribute("label")) ?
              bundle.getString(((Element) children[i]).getAttribute("label")) :
              getText((Element) children[i], bundle),
            "true".equals(((Element) children[i]).getAttribute("selected"))
          );

        if (add != null)
        {
          add.add(option);
        }

        if (select != null && option.selected)
        {
          select.select(option);
        }
      }

      addOptions(children[i], bundle, add, select);
    }
  }



  private JComponent
  br(Element element, ResourceBundle bundle)
  {
    // Use two subpanels to get the line in the middle.

    JComponent	bottom = new JPanel();
    JComponent	result = new JPanel(new BorderLayout());
    JComponent	top = new JPanel();

    bottom.setBackground(getBackground());
    top.setBackground(getBackground());
    result.setBackground(getBackground());
    top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
    result.add(top, BorderLayout.NORTH);
    result.add(bottom, BorderLayout.SOUTH);

    return result;
  }



  private JComponent
  createComboBox(final Element element, ResourceBundle bundle)
  {
    final Element	el = element;
    final JComboBox	combo =
      new JComboBox(new DefaultComboBoxModel())
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    controls.put(element.getAttribute("name"), combo);
    combo.setEditable("true".equals(element.getAttribute("editable")));

    if ("mime-type".equals(element.getAttribute("type")))
    {
      combo.setRenderer(new MimeTypeOptionListCellRenderer());
    }

    combo.addItemListener
    (
      new ItemListener()
      {
        public void
        itemStateChanged(ItemEvent e)
        {
          Object	item = combo.getSelectedItem();

          if (item instanceof Option)
          {
            ((Option) item).selected = true;
          }

          updateValue
          (
            el.getAttribute("name"),
            combo,
            item instanceof Option ?
              item : new Option(item.toString(), item.toString(), true)
          );
        }
      }
    );

    addOptions
    (
      element,
      bundle,
      new AddOption()
      {
        public void
        add(Option option)
        {
          ((DefaultComboBoxModel) combo.getModel()).addElement(option);
        }
      },
      new SelectOption()
      {
        public void
        select(Option option)
        {
          combo.setSelectedIndex
          (
            ((DefaultComboBoxModel) combo.getModel()).getIndexOf(option)
          );
        }
      }
    );

    return combo;
  }



  private JComponent
  createListBox(final Element element, ResourceBundle bundle)
  {
    final Element	el = element;
    LongList		longList =
      "true".equals(element.getAttribute("long")) ? createLongList() : null;
    final JList		list =
      longList != null ?
        longList.getList() :
        new JList(new DefaultListModel())
        {
          public Dimension
          getPreferredSize()
          {
            return getRealPreferredSize(element, super.getPreferredSize());
          }
        };

    controls.put(element.getAttribute("name"), list);

    if (!"".equals(element.getAttribute("size")))
    {
      list.setVisibleRowCount(Integer.parseInt(element.getAttribute("size")));
    }

    if ("mime-type".equals(element.getAttribute("type")))
    {
      list.setCellRenderer(new MimeTypeListCellRenderer());
    }

    list.setSelectionMode
    (
      "true".equalsIgnoreCase(element.getAttribute("multiple")) ?
        ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
        ListSelectionModel.SINGLE_SELECTION
    );

    list.addListSelectionListener
    (
      new ListSelectionListener()
      {
        public void
        valueChanged(ListSelectionEvent e)
        {
          updateValue
          (
            el.getAttribute("name"),
            list,
            list.getSelectedValues()
          );
        }
      }
    );

    addOptions
    (
      element,
      bundle,
      new AddOption()
      {
        public void
        add(Option option)
        {
          ((DefaultListModel) list.getModel()).addElement(option);
        }
      },
      new SelectOption()
      {
        public void
        select(Option option)
        {
          list.addSelectionInterval
          (
            ((DefaultListModel) list.getModel()).indexOf(option),
            ((DefaultListModel) list.getModel()).indexOf(option)
          );
        }
      }
    );

    return longList != null ? longList : createScrollableList(list);
  }



  private static LongList
  createLongList()
  {
    return
      new LongList()
      {
        public Dimension
        getPreferredSize()
        {
          Rectangle	cellBounds = getList().getCellBounds(0, 0);

          return
            new Dimension
            (
              (int)
                Math.max
                (
                  150,
                  getList().getPreferredSize().getWidth() +
                    getScrollPane().getVerticalScrollBar().getPreferredSize().
                    getWidth() +
                    5
                ),
              getList().getVisibleRowCount() *
                (int)
                  (
                    cellBounds != null ?
                      cellBounds.getHeight() :
                      getList().getFontMetrics(getList().getFont()).getHeight()
                  )
            );
        }
      };
  }



  private static JScrollPane
  createScrollableList(final JList list)
  {

    return
      new JScrollPane(list)
      {
        public Dimension
        getPreferredSize()
        {
          Rectangle	cellBounds = list.getCellBounds(0, 0);

          return
            new Dimension
            (
              (int)
                Math.max
                (
                  150,
                  list.getPreferredSize().getWidth() +
                    getVerticalScrollBar().getPreferredSize().getWidth() + 5
                ),
              list.getVisibleRowCount() *
                (int)
                  (
                    cellBounds != null ?
                      cellBounds.getHeight() :
                      list.getFontMetrics(list.getFont()).getHeight()
                  )
            );
          }
      };
  }



  private JComponent
  createTransferBox(final Element element, ResourceBundle bundle)
  {
    JPanel	buttons = new JPanel();
    JButton	deselectButton = new JButton("<");
    LongList	longList =
      "true".equals(element.getAttribute("long")) ? createLongList() : null;
    final JList	notSelected =
      longList != null ? longList.getList() : new JList(new DefaultListModel());
    JPanel	result =
      new JPanel()
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };
    JButton	selectButton = new JButton(">");
    final JList	selected = new JList(new DefaultListModel());

    if (!"".equals(element.getAttribute("size")))
    {
      selected.
        setVisibleRowCount(Integer.parseInt(element.getAttribute("size")));
      notSelected.
        setVisibleRowCount(Integer.parseInt(element.getAttribute("size")));
    }

    selected.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    notSelected.
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    selectButton.setBackground(getBackground());
    deselectButton.setBackground(getBackground());
    buttons.setBackground(getBackground());
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
    buttons.add(selectButton);
    buttons.add(Box.createRigidArea(new Dimension(0, 5)));
    buttons.add(deselectButton);

    selectButton.addActionListener
    (
      new TransferAction
      (
        notSelected,
        selected,
        selected,
        element.getAttribute("name")
      )
    );

    deselectButton.addActionListener
    (
      new TransferAction
      (
        selected,
        notSelected,
        selected,
        element.getAttribute("name")
      )
    );

    selected.getModel().addListDataListener
    (
      new TransferSelectedListener(selected, element.getAttribute("name"))
    );

    addOptions
    (
      element,
      bundle,
      new AddOption()
      {
        public void
        add(Option option)
        {
          if (option.selected)
          {
            ((DefaultListModel) selected.getModel()).addElement(option);
          }
          else
          {
            ((DefaultListModel) notSelected.getModel()).addElement(option);
          }
        }
      },
      null
    );

    result.setBackground(getBackground());
    result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
    result.add(longList != null ? longList : createScrollableList(notSelected));
    result.add(Box.createRigidArea(new Dimension(5, 0)));
    result.add(buttons);
    result.add(Box.createRigidArea(new Dimension(5, 0)));
    result.add(createScrollableList(selected));

    return result;
  }



  private JComponent
  div(final Element element, ResourceBundle bundle)
  {
    JComponent	result =
      new JPanel(new GridBagLayout())
      {
        public Dimension
        getMaximumSize()
        {
          return getRealPreferredSize(element, super.getMaximumSize());
        }

        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    result.setBackground(getBackground());
    processFlowArea(result, element, bundle);

    return result;
  }



  private static Collection
  expandValues(Collection values)
  {
    List	result = new ArrayList();

    for (Object value: values)
    {
      if (value.getClass().isArray())
      {
        result.addAll(expandValues(Arrays.asList((Object[]) value)));
      }
      else
      {
        if (value instanceof Collection)
        {
          result.addAll(expandValues((Collection) value));
        }
        else
        {
          result.add(value instanceof Option ? ((Option) value).value : value);
        }
      }
    }

    return result;
  }



  private static JComponent
  fillBelow(JComponent component)
  {
    JPanel	panel = new JPanel(new BorderLayout());

    panel.add(component, BorderLayout.NORTH);

    return panel;
  }



  private static void
  flattenColgroups(Element table)
  {
    Node[]	colgroups =
      be.re.xml.Util.selectChildren(table, XHTML, "colgroup");

    for (int i = 0; i < colgroups.length; ++i)
    {
      Node[]	cols =
        be.re.xml.Util.selectChildren(colgroups[i], XHTML, "col");

      for (int j = 0; j < cols.length; ++j)
      {
        inheritColAttribute
        (
          (Element) colgroups[i],
          (Element) cols[j],
          "align",
          "left"
        );

        inheritColAttribute
        (
          (Element) colgroups[i],
          (Element) cols[j],
          "valign",
          "middle"
        );

        inheritColAttribute
        (
          (Element) colgroups[i],
          (Element) cols[j],
          "width",
          "1*"
        );

        table.insertBefore(cols[j], colgroups[i]);
      }

      if (!"".equals(((Element) colgroups[i]).getAttribute("span")))
      {
        int	span =
          Integer.parseInt(((Element) colgroups[i]).getAttribute("span"));

        for (int j = cols.length; j < span; ++j)
        {
          Element	col =
            table.getOwnerDocument().createElementNS(XHTML, "col");

          inheritColAttribute((Element) colgroups[i], col, "align", "left");
          inheritColAttribute((Element) colgroups[i], col, "valign", "middle");
          inheritColAttribute((Element) colgroups[i], col, "width", "1*");
          table.insertBefore(col, colgroups[i]);
        }
      }

      table.removeChild(colgroups[i]);
    }
  }



  private static int
  getCellAlign(Element cell, Element column)
  {
    String	align = inheritCellAlign(cell, column, "align", "left");
    String	valign = inheritCellAlign(cell, column, "valign", "middle");

    return
      "top".equals(valign) && "left".equals(align) ?
        GridBagConstraints.NORTHWEST :
        (
          "top".equals(valign) && "center".equals(align) ?
          GridBagConstraints.NORTH :
          (
            "top".equals(valign) && "right".equals(align) ?
            GridBagConstraints.NORTHEAST :
            (
              "middle".equals(valign) && "left".equals(align) ?
              GridBagConstraints.WEST :
              (
                "middle".equals(valign) && "center".equals(align) ?
                GridBagConstraints.CENTER :
                (
                  "middle".equals(valign) && "right".equals(align) ?
                  GridBagConstraints.EAST :
                  (
                    "bottom".equals(valign) && "left".equals(align) ?
                    GridBagConstraints.SOUTHWEST :
                    (
                      "bottom".equals(valign) && "center".equals(align) ?
                      GridBagConstraints.SOUTH :
                      (
                        "bottom".equals(valign) && "right".equals(align) ?
                        GridBagConstraints.SOUTHEAST : GridBagConstraints.CENTER
                      )
                    )
                  )
                )
              )
            )
          )
        );
  }



  private static double
  getColumnWidth(Element column)
  {
    String	width = column.getAttribute("width");

    return
      width.endsWith("*") ?
        Double.parseDouble(width.substring(0, width.indexOf('*'))) : 1.0;
  }



  public Object
  getControl(String field)
  {
    return controls.get(field);
  }



  private static int
  getDimension(String value)
  {
    int	index = value.indexOf("px");

    if (index != -1)
    {
      value = value.substring(0, index);
    }

    return Integer.parseInt(value);
  }



  public String[]
  getFieldNames()
  {
    return controls.keySet().toArray(new String[0]);
  }



  /**
   * Returns a map of field names to <code>Object[]</code> values.
   */

  public Map<String,Object[]>
  getFields()
  {
    Map<String,Object[]>	result = new HashMap<String,Object[]>();

    for (String field: fields.keySet())
    {
      result.put(field, getValues(field));
    }

    return result;
  }



  private static File
  getFile(boolean directoryOnly, boolean hiding)
  {
    JFileChooser	chooser = new JFileChooser();

    chooser.setMultiSelectionEnabled(false);

    if (directoryOnly)
    {
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    chooser.setFileHidingEnabled(hiding);

    return
      chooser.showOpenDialog(SimpleFormDialog.getDefaultOwner()) !=
        JFileChooser.APPROVE_OPTION ?
        null : chooser.getSelectedFile();
  }



  private static String
  getFontStyle(Element element)
  {
    return getStyle(element, "font-style", true, "normal");
  }



  private static String
  getFontWeight(Element element)
  {
    return getStyle(element, "font-weight", true, "normal");
  }



  private static int
  getMargin(Element element, String side, String defaultValue)
  {
    return
      getDimension
      (
        getStyle
        (
          element,
          new String[]{"margin-" + side, "margin"},
          defaultValue
        )
      );
  }



  private static int
  getPadding(Element element, String defaultValue)
  {
    return
      getDimension(getStyle(element, new String[]{"padding"}, defaultValue));
  }



  private static Dimension
  getRealPreferredSize(Element element, Dimension preferred)
  {
    setSizes(element, preferred);

    return preferred;
  }



  private static String
  getStyle(Element element, String[] properties, String defaultValue)
  {
    String		result = null;
    Map<String,String>	values = getStyleProperties(element);

    for (int i = 0; i < properties.length && result == null; ++i)
    {
      result = values.get(properties[i]);
    }

    return result != null ? result : defaultValue;
  }



  private static String
  getStyle
  (
    Element	element,
    String	property,
    boolean	inherit,
    String	defaultValue
  )
  {
    if (element == null)
    {
      return defaultValue;
    }

    String	value = getStyle(element, new String[]{property}, null);

    return
      value != null ?
        value :
        (
          inherit ?
            getStyle
            (
              (Element) element.getParentNode(),
              property,
              true,
              defaultValue
            ) : defaultValue
        );
  }



  private static Map<String,String>
  getStyleProperties(Element element)
  {
    String	style = element.getAttribute("style");

    if ("".equals(style))
    {
      return new HashMap<String,String>();
    }

    Map			result = new HashMap<String,String>();
    StringTokenizer	tokenizer = new StringTokenizer(style, ";");

    while (tokenizer.hasMoreTokens())
    {
      String	property = tokenizer.nextToken();
      int	colon = property.indexOf(':');

      if (colon != -1)
      {
        result.put
        (
          property.substring(0, colon).trim(),
          property.substring(colon + 1).trim()
        );
      }
    }

    return result;
  }



  private static String
  getText(Element element, ResourceBundle bundle)
  {
    String	text = null;

    if (bundle != null && !"".equals(element.getAttribute("id")))
    {
      try
      {
        text = bundle.getString(element.getAttribute("id"));
      }

      catch (MissingResourceException e)
      {
      }
    }

    return text != null ? text : be.re.xml.Util.getText(element);
  }



  private static String
  getTextAlign(Element element)
  {
    return getStyle(element, "text-align", true, "left");
  }



  public Object[]
  getValues(String field)
  {
    return expandValues(fields.get(field).values()).toArray();
  }



  private static Node
  groupInlineElements(Node node)
  {
    Node	currentGroup = null;
    Element[]	elements = selectElements(node);

    for (int i = 0; i < elements.length; ++i)
    {
      if (Array.inArray(INLINE, elements[i].getLocalName()))
      {
        if (currentGroup == null)
        {
          currentGroup =
            node.insertBefore
            (
              node.getOwnerDocument().createElementNS(XHTML, "inline"),
              elements[i]
            );
        }

        currentGroup.appendChild(elements[i]);
      }
      else
      {
        currentGroup = null;
        groupInlineElements(elements[i]);
      }
    }

    return node;
  }



  private JComponent
  h1(final Element element, ResourceBundle bundle)
  {
    return p(element, bundle, 1.4f);
  }



  private JComponent
  h2(final Element element, ResourceBundle bundle)
  {
    return p(element, bundle, 1.2f);
  }



  private JComponent
  h3(final Element element, ResourceBundle bundle)
  {
    return p(element, bundle, 1.1f);
  }



  private static String
  inheritCellAlign
  (
    Element	cell,
    Element	column,
    String	attribute,
    String	defaultValue
  )
  {
    String	result = cell.getAttribute(attribute);

    if ("".equals(result) && column != null)
    {
      result = column.getAttribute(attribute);
    }

    for
    (
      Element parent = (Element) cell.getParentNode();
      parent != null && "".equals(result);
      parent = (Element) parent.getParentNode()
    )
    {
      result = parent.getAttribute(attribute);
    }

    return "".equals(result) ? defaultValue : result;
  }



  private static void
  inheritColAttribute
  (
    Element	colgroup,
    Element	col,
    String	attribute,
    String	defaultValue
  )
  {
    if ("".equals(col.getAttribute(attribute)))
    {
      col.setAttribute
      (
        attribute,
        !"".equals(colgroup.getAttribute(attribute)) ?
          colgroup.getAttribute(attribute) : defaultValue
      );
    }
  }



  private JComponent
  inline(final Element element, ResourceBundle bundle)
  {
    Element[]	elements = selectElements(element);
    String	textAlign = getTextAlign(element);
    JPanel	result =
      new JPanel
      (
        new FlowLayout
        (
          "center".equalsIgnoreCase(textAlign) ?
            FlowLayout.CENTER :
            (
              "right".equalsIgnoreCase(textAlign) ?
                FlowLayout.RIGHT: FlowLayout.LEFT
            ),
            2,
            2
        )
      )
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    result.setBackground(getBackground());

    for (int i = 0; i < elements.length; ++i)
    {
      result.add(processElement(elements[i], bundle));
    }

    return result;
  }



  private JComponent
  input(Element element, ResourceBundle bundle)
  {
    return processInputControl(element, bundle);
  }



  private JComponent
  inputCheckbox(final Element element, ResourceBundle bundle)
  {
    final JCheckBox	result =
      new JCheckBox()
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    controls.put(element.getAttribute("name"), result);

    result.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          updateValue
          (
            element.getAttribute("name"),
            result,
            new Boolean(result.isSelected())
          );
        }
      }
    );

    boolean	value =
      "true".equalsIgnoreCase(element.getAttribute("checked"));

    result.setSelected(value);
    updateValue(element.getAttribute("name"), result, new Boolean(value));

    return result;
  }



  private JComponent
  inputDatetime(final Element element, ResourceBundle bundle)
  {
    final DateTimePicker	result = new DateTimePicker();

    controls.put(element.getAttribute("name"), result);

    if (!"".equals(element.getAttribute("value")))
    {
      result.setDate
      (
        new Date(be.re.util.Util.parseTimestamp(element.getAttribute("value")))
      );
    }

    result.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          try
          {
            updateValue
            (
              element.getAttribute("name"),
              e.getSource(),
              result.getDate()
            );
          }

          catch (Exception ex)
          {
            throw new RuntimeException(ex);
          }
        }
      }
    );

    return result;
  }



  private JComponent
  inputDirectory(final Element element, ResourceBundle bundle)
  {
    return
      inputFileOrDirectory
      (
        element,
        true,
        !"false".equalsIgnoreCase(element.getAttribute("hiding")),
        bundle
      );
  }



  private JComponent
  inputFile(final Element element, ResourceBundle bundle)
  {
    return
      inputFileOrDirectory
      (
        element,
        false,
        !"false".equalsIgnoreCase(element.getAttribute("hiding")),
        bundle
      );
  }



  private JComponent
  inputFileOrDirectory
  (
    final Element	element,
    final boolean	directoryOnly,
    final boolean	hiding,
    ResourceBundle	bundle
  )
  {
    JPanel		result =
      new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0))
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };
    final JTextField	text = (JTextField) inputText(element, bundle);

    result.setBackground(getBackground());
    result.add(text);

    JButton	button = new JButton(Util.getResource("label_browse"));

    button.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          File	file = getFile(directoryOnly, hiding);

          if (file != null)
          {
            text.setText(file.getAbsolutePath());
          }
        }
      }
    );

    button.setBackground(getBackground());
    result.add(button);

    return result;
  }



  private JComponent
  inputPassword(Element element, ResourceBundle bundle)
  {
    return
      inputText
      (
        element,
        new JPasswordField()
        {
          public void
          paste()
          {
          }
        }
      );
  }



  private JComponent
  inputRadio(final Element element, ResourceBundle bundle)
  {
    ButtonGroup		group = groups.get(element.getAttribute("name"));
    final JRadioButton	result =
      new JRadioButton()
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    if (group == null)
    {
      group = new ButtonGroup();
      groups.put(element.getAttribute("name"), group);
    }

    controls.put(element.getAttribute("name"), group);

    final ButtonGroup	g = group;

    result.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          updateValue
          (
            element.getAttribute("name"),
            g,
            element.getAttribute("value")
          );
        }
      }
    );

    if ("true".equalsIgnoreCase(element.getAttribute("checked")))
    {
      result.setSelected(true);

      updateValue
      (
        element.getAttribute("name"),
        g,
        element.getAttribute("value")
      );
    }

    result.setName(element.getAttribute("value"));
    result.setBackground(getBackground());
    group.add(result);

    return result;
  }



  private JComponent
  inputText(final Element element, ResourceBundle bundle)
  {
    return
      be.re.util.Util.isInteger(element.getAttribute("min")) &&
        be.re.util.Util.isInteger(element.getAttribute("max")) ?
        slider(element) :
        inputText
        (
          element,
          new JTextField()
          {
            public Dimension
            getPreferredSize()
            {
              return getRealPreferredSize(element, super.getPreferredSize());
            }
          }
        );
  }



  private JComponent
  inputText(Element element, JTextField textField)
  {
    controls.put(element.getAttribute("name"), textField);

    textField.getDocument().addDocumentListener
    (
      new TextListener(element.getAttribute("name"), textField)
    );

    if (!"".equals(element.getAttribute("value")))
    {
      textField.setText(element.getAttribute("value"));
    }

    textField.setColumns
    (
      !"".equals(element.getAttribute("size")) ?
        Integer.parseInt(element.getAttribute("size")) : 10
    );

    return textField;
  }



  private JComponent
  label(final Element element, ResourceBundle bundle)
  {
    JComponent	result =
      new JLabel(getText(element, bundle))
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    if (!"".equals(element.getAttribute("title")))
    {
      result.setToolTipText(element.getAttribute("title"));
    }

    return result;
  }



  private JComponent
  p(final Element element, ResourceBundle bundle)
  {
    JTextPane	result =
      new JTextPane()
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };

    result.setText(getText(element, bundle));
    result.setEditable(false);
    result.setFocusable(false);

    String		alignment = getTextAlign(element);
    SimpleAttributeSet	attributes = new SimpleAttributeSet();

    StyleConstants.setAlignment
    (
      attributes,
      "left".equals(alignment) ?
        StyleConstants.ALIGN_LEFT :
        (
          "right".equals(alignment) ?
            StyleConstants.ALIGN_RIGHT :
            (
              "center".equals(alignment) ?
                StyleConstants.ALIGN_CENTER : StyleConstants.ALIGN_JUSTIFIED
            )
        )
    );

    StyleConstants.setBold(attributes, "bold".equals(getFontWeight(element)));
    StyleConstants.
      setItalic(attributes, "italic".equals(getFontStyle(element)));
    result.setParagraphAttributes(attributes, false);
    result.setBackground(getBackground());

    return result;
  }



  private JComponent
  p(final Element element, ResourceBundle bundle, float fontSizeFactor)
  {
    JComponent	result = p(element, bundle);

    result.setFont
    (
      result.getFont().deriveFont(fontSizeFactor * result.getFont().getSize2D())
    );

    return result;
  }



  private JComponent
  processElement(Element element, ResourceBundle bundle)
  {
    try
    {
      final JComponent	result =
        (JComponent)
          getClass().getDeclaredMethod
          (
            element.getLocalName(),
            new Class[]{Element.class, ResourceBundle.class}
          ).invoke(this, new Object[]{element, bundle});

      if (debug)
      {
        result.setBorder(BorderFactory.createLineBorder(Color.BLACK));
      }

      final String	height =
        getStyle(element, new String[]{"height"}, null);
      String		overflow =
        getStyle(element, new String[]{"overflow"}, null);
      final String	width =
        getStyle(element, new String[]{"width"}, null);

      if ("scroll".equals(overflow) && (height != null || width != null))
      {
        JScrollPane	pane =
          new JScrollPane(fillBelow(result))
          {
            public Dimension
            getPreferredSize()
            {
              return
                new Dimension
                (
                  (
                    width != null ?
                      Integer.parseInt(width) :
                      super.getPreferredSize().width
                  ) + getHorizontalScrollBar().getPreferredSize().width,
                  height != null ?
                    Integer.parseInt(height) :
                    super.getPreferredSize().height
                );
            }

            public Dimension
            getMinimumSize()
            {
              return
                new Dimension
                (
                  width != null ?
                    Integer.parseInt(width) :
                    super.getMinimumSize().width,
                  height != null ?
                    Integer.parseInt(height) :
                    super.getMinimumSize().height
                );
            }
          };

        pane.setBorder(null);

        return pane;
      }

      return result;
    }

    catch (Exception e)
    {
      be.re.util.Util.printStackTrace(e);

      return null;
    }
  }



  private void
  processFlowArea(JComponent component, Node node, ResourceBundle bundle)
  {
    Element[]	elements = selectElements(node);

    for (int i = 0; i < elements.length; ++i)
    {
      JComponent	child = processElement(elements[i], bundle);

      component.add
      (
        child,
        new GridBagConstraints
        (
          0,
          i,
          1,
          1,
          1.0,
          0.0,
          GridBagConstraints.WEST,
          GridBagConstraints.HORIZONTAL,
          new Insets
          (
            getMargin(elements[i], "top", "5"),
            getMargin(elements[i], "left", "5"),
            getMargin(elements[i], "bottom", "5"),
            getMargin(elements[i], "right", "5")
          ),
          getPadding(elements[i], "0"),
          getPadding(elements[i], "0")
        )
      );
    }
  }



  private JComponent
  processInputControl(Element element, ResourceBundle bundle)
  {
    String	type = element.getAttribute("type");

    type =
      "".equals(type) ?
        "Text" : (type.substring(0, 1).toUpperCase() + type.substring(1));

    try
    {
      return
        (JComponent)
          getClass().getDeclaredMethod
          (
            "input" + type,
            new Class[]{Element.class, ResourceBundle.class}
          ).invoke(this, new Object[]{element, bundle});
    }

    catch (Exception e)
    {
      be.re.util.Util.printStackTrace(e);

      return null;
    }
  }



  public void
  removeChangeListener(ChangeListener listener)
  {
    changeListeners.remove(listener);
  }



  private JComponent
  select(Element element, ResourceBundle bundle)
  {
    return
      "true".equals(element.getAttribute("multiple")) ?
        (
          "true".equals(element.getAttribute("transfer")) ?
            createTransferBox(element, bundle) : createListBox(element, bundle)
        ) :
        (
          "true".equals(element.getAttribute("long")) ?
            createListBox(element, bundle) : createComboBox(element, bundle)
        );
  }



  private static Element[]
  selectElements(Node node)
  {
    Element[]	elements = be.re.xml.Util.selectElements(node);
    List	result = new ArrayList();

    for (int i = 0; i < elements.length; ++i)
    {
      if (XHTML.equals(elements[i].getNamespaceURI()))
      {
        result.add(elements[i]);
      }
    }

    return (Element[]) result.toArray(new Element[0]);
  }



  private static Element[]
  selectTableRows(Element table)
  {
    List	result = new ArrayList();
    Node[]	bodies = be.re.xml.Util.selectChildren(table, XHTML, "tbody");

    for (int i = 0; i < bodies.length; ++i)
    {
      result.addAll
      (
        Arrays.asList(be.re.xml.Util.selectChildren(bodies[i], XHTML, "tr"))
      );
    }

    return (Element[]) result.toArray(new Element[0]);
  }



  private void
  sendChangeEvent(String field)
  {
    changeListeners.dispatch(new ChangeEvent(this, getFields(), field));
  }



  public void
  setDocument(Document document)
  {
    removeAll();

    if
    (
      document == null							||
      !"form".equals(document.getDocumentElement().getLocalName())	||
      !XHTML.equals(document.getDocumentElement().getNamespaceURI())
    )
    {
      return;
    }

    processFlowArea
    (
      this,
      groupInlineElements(document.getDocumentElement().cloneNode(true)),
      bundle
    );
  }



  private static void
  setSizes(Element element, Dimension preferred)
  {
    String	minHeight = getStyle(element, new String[]{"min-height"}, null);
    String	minWidth = getStyle(element, new String[]{"min-width"}, null);
    String	maxHeight = getStyle(element, new String[]{"max-height"}, null);
    String	maxWidth = getStyle(element, new String[]{"max-width"}, null);

    if (minHeight != null && getDimension(minHeight) > preferred.height)
    {
      preferred.height = getDimension(minHeight);
    }

    if (minWidth != null && getDimension(minWidth) > preferred.width)
    {
      preferred.width = getDimension(minWidth);
    }

    if (maxHeight != null && getDimension(maxHeight) < preferred.height)
    {
      preferred.height = getDimension(maxHeight);
    }

    if (maxWidth != null && getDimension(maxWidth) < preferred.width)
    {
      preferred.width = getDimension(maxWidth);
    }
  }



  private JComponent
  slider(final Element element)
  {
    int		max = Integer.parseInt(element.getAttribute("max"));
    int		min = Integer.parseInt(element.getAttribute("min"));
    String	value = element.getAttribute("value");

    final JSlider	result =
      be.re.util.Util.isInteger(value) && Integer.parseInt(value) <= max &&
        Integer.parseInt(value) >= min ?
        new JSlider(min, max, Integer.parseInt(value)) : new JSlider(min, max);

    result.setLabelTable(result.createStandardLabels(1));
    result.setPaintLabels(true);
    result.setPaintTicks(true);
    result.setSnapToTicks(true);

    result.addChangeListener
    (
      new javax.swing.event.ChangeListener()
      {
        public void
        stateChanged(javax.swing.event.ChangeEvent e)
        {
          updateValue
          (
            element.getAttribute("name"),
            result,
            new Integer(result.getValue())
          );
        }
      }
    );

    return result;
  }



  private JComponent
  span(final Element element, ResourceBundle bundle)
  {
    return inline(element, bundle);
  }



  private JComponent
  table(final Element element, ResourceBundle bundle)
  {
    String	cellPadding =
      "".equals(element.getAttribute("cellpadding")) ?
        "0" : element.getAttribute("cellpadding");
    String	cellSpacing =
      "".equals(element.getAttribute("cellspacing")) ?
        "0" : element.getAttribute("cellspacing");
    JPanel	result =
      new JPanel(new GridBagLayout())
      {
        public Dimension
        getPreferredSize()
        {
          return getRealPreferredSize(element, super.getPreferredSize());
        }
      };
    Node[]	rows = selectTableRows(element);

    result.setBackground(getBackground());
    flattenColgroups(element);

    Node[]	columns = be.re.xml.Util.selectChildren(element, XHTML, "col");

    for (int i = 0; i < rows.length; ++i)
    {
      Node[]	cells = be.re.xml.Util.selectChildren(rows[i], XHTML, "td");

      for (int j = 0; j < cells.length; ++j)
      {
        result.add
        (
          td((Element) cells[j], bundle),
          new GridBagConstraints
          (
            j,
            i,
            !"".equals(((Element) cells[j]).getAttribute("colspan")) ?
              Integer.
                parseInt(((Element) cells[j]).getAttribute("colspan")) : 1,
            !"".equals(((Element) cells[j]).getAttribute("rowspan")) ?
              Integer.
                parseInt(((Element) cells[j]).getAttribute("colspan")) : 1,
            j < columns.length ? getColumnWidth((Element) columns[j]) : 1.0,
            0.0,
            getCellAlign
            (
              (Element) cells[j],
              j < columns.length ? (Element) columns[j] : null
            ),
            GridBagConstraints.NONE,
            new Insets
            (
              getMargin((Element) cells[j], "top", cellSpacing),
              getMargin((Element) cells[j], "left", cellSpacing),
              getMargin((Element) cells[j], "bottom", cellSpacing),
              getMargin((Element) cells[j], "right", cellSpacing)
            ),
            getPadding((Element) cells[j], cellPadding),
            getPadding((Element) cells[j], cellPadding)
          )
        );
      }
    }

    return result;
  }



  private JComponent
  td(Element element, ResourceBundle bundle)
  {
    JComponent	result = new JPanel(new GridBagLayout());

    result.setBackground(getBackground());
    processFlowArea(result, element, bundle);

    return result;
  }



  private JComponent
  textarea(final Element element, ResourceBundle bundle)
  {
    boolean	editable =
      !(
        "".equals(element.getAttribute("readonly")) ||
          "true".equals(element.getAttribute("readonly"))
      );
    JTextArea	textArea =
      !"".equals(element.getAttribute("rows")) &&
        !"".equals(element.getAttribute("cols")) ?
        new JTextArea
        (
          Integer.parseInt(element.getAttribute("rows")),
          Integer.parseInt(element.getAttribute("cols"))
        )
        {
          public Dimension
          getPreferredSize()
          {
            return getRealPreferredSize(element, super.getPreferredSize());
          }
        } :
        new JTextArea()
        {
          public Dimension
          getPreferredSize()
          {
            return getRealPreferredSize(element, super.getPreferredSize());
          }
        };
    JScrollPane	result = new JScrollPane(textArea);

    controls.put(element.getAttribute("name"), textArea);

    if (!editable)
    {
      textArea.setBorder(BorderFactory.createEmptyBorder());
      textArea.setBackground(getBackground());
      result.setBorder(BorderFactory.createEmptyBorder());
    }

    textArea.setEditable(editable);

    boolean	wrap =
      "normal".equals(getStyle(element, new String[]{"white-space"}, "normal"));

    textArea.setLineWrap(wrap);
    textArea.setWrapStyleWord(wrap);

    textArea.getDocument().addDocumentListener
    (
      new TextListener(element.getAttribute("name"), textArea)
    );

    String	text = getText(element, bundle);

    if (text != null)
    {
      textArea.setText(text);
    }

    return result;
  }



  private void
  updateValue(String field, Object source, Object value)
  {
    Map	map = fields.get(field);

    if (map == null)
    {
      map = new HashMap();
      fields.put(field, map);
    }

    map.put(source, value);
    sendChangeEvent(field);
  }



  public void
  updateValue(String field, Object value)
  {
    Map	map = fields.get(field);

    if (map == null)
    {
      map = new HashMap();
      fields.put(field, map);
    }

    map.put(controls.get(field), value);

    for (Object source: map.keySet())
    {
      boolean	found = false;

      for
      (
        Class c = source.getClass();
        c != null && !found;
        c = c.getSuperclass()
      )
      {
        try
        {
          getClass().getDeclaredMethod
          (
            "updateValue",
            new Class[]{c, String.class, Object.class}
          ).invoke(this, new Object[]{source, field, value});

          found = true;
        }

        catch (Exception e)
        {
        }
      }
    }
  }



  private void
  updateValue(ButtonGroup component, String field, Object value)
  {
    Enumeration<AbstractButton>	e = component.getElements();

    while (e.hasMoreElements())
    {
      AbstractButton	button = e.nextElement();

      button.setSelected(button.getName().equals(value));
    }
  }



  private void
  updateValue(JCheckBox component, String field, Object value)
  {
    component.setSelected(((Boolean) value).booleanValue());
  }



  private void
  updateValue(JComboBox component, String field, Object value)
  {
    for (int i = 0; i < component.getItemCount(); ++i)
    {
      Object	item = component.getItemAt(i);

      if (item instanceof Option && ((Option) item).value.equals(value))
      {
        ((Option) item).selected = true;
        component.setSelectedIndex(i);

        return;
      }
    }

    component.insertItemAt(value, 0);
    component.setSelectedIndex(0);
  }



  private void
  updateValue(JList component, String field, Object value)
  {
    String[]	values =
      value instanceof String ?
        new String[]{(String) value} :
        (
          value instanceof String[] ?
            (
              component.getSelectionMode() ==
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION ?
                (String[]) value : new String[]{((String[]) value)[0]}
            ) : new String[0]
        );

    component.setValueIsAdjusting(true);
    component.clearSelection();

    for (int i = 0; i < values.length; ++i)
    {
      for (int j = 0; j < component.getModel().getSize(); ++j)
      {
        if
        (
          values[i].
            equals(((Option) component.getModel().getElementAt(j)).value)
        )
        {
          component.setSelectedIndex(j);
          ((Option) component.getModel().getElementAt(j)).selected = true;
        }
      }
    }

    component.setValueIsAdjusting(false);
  }



  private void
  updateValue(JSlider component, String field, Object value)
  {
    component.setValue(((Integer) value).intValue());
  }



  private void
  updateValue(JTextArea component, String field, Object value)
  {
    component.setText((String) value);
    component.setCaretPosition(0);
  }



  private void
  updateValue(JTextField component, String field, Object value)
  {
    component.setText((String) value);
    component.setCaretPosition(0);
  }



  /**
   * Sets the background to that used by <q>p</q> elements, which gives a more
   * uniform look.
   */

  public void
  useTextBackground()
  {
    setBackground(UIManager.getColor("TextPane.background"));
  }



  private interface AddOption

  {

    public void	add	(Option option);

  } // AddOption



  private interface SelectOption

  {

    public void	select	(Option option);

  } // SelectOption



  private static class MimeTypeOptionListCellRenderer
    extends MimeTypeListCellRenderer

  {

    public Component
    getListCellRendererComponent
    (
      JList	list,
      Object	value,
      int	index,
      boolean	isSelected,
      boolean	cellHasFocus
    )
    {
      return
        super.getListCellRendererComponent
        (
          list,
          new MimeType.LabeledMimeType
          (
            new String[]{((Option) value).value.toString()},
            ((Option) value).label
          ),
          index,
          isSelected,
          cellHasFocus
        );
    }

  } // MimeTypeOptionListCellRenderer



  private class TextListener implements DocumentListener

  {

    private JComponent	component;
    private String	name;



    private
    TextListener(String name, JComponent component)
    {
      this.name = name;
      this.component = component;
    }



    public void
    changedUpdate(DocumentEvent e)
    {
      updateValue(name, component, getText(e));
    }



    private String
    getText(DocumentEvent e)
    {
      try
      {
        return e.getDocument().getText(0, e.getDocument().getLength());
      }

      catch (BadLocationException ex)
      {
        throw new RuntimeException(ex);
      }
    }



    public void
    insertUpdate(DocumentEvent e)
    {
      updateValue(name, component, getText(e));
    }



    public void
    removeUpdate(DocumentEvent e)
    {
      updateValue(name, component, getText(e));
    }

  } // TextListener



  private class TransferAction implements ActionListener

  {

    private JList	from;
    private String	name;
    private JList	source;
    private JList	to;



    private
    TransferAction(JList from, JList to, JList source, String name)
    {
      this.from = from;
      this.to = to;
      this.source = source;
      this.name = name;
    }



    public void
    actionPerformed(ActionEvent e)
    {
      Object[]	values = from.getSelectedValues();

      for (int i = 0; i < values.length; ++i)
      {
        ((DefaultListModel) from.getModel()).removeElement(values[i]);
        ((DefaultListModel) to.getModel()).addElement(values[i]);
      }
    }

  } // TransferAction



  private class TransferSelectedListener implements ListDataListener

  {

    private String	name;
    private JList	selected;



    private
    TransferSelectedListener(JList selected, String name)
    {
      this.selected = selected;
      this.name = name;
    }



    public void
    contentsChanged(ListDataEvent e)
    {
      update();
    }



    public void
    intervalAdded(ListDataEvent e)
    {
      update();
    }



    public void
    intervalRemoved(ListDataEvent e)
    {
      update();
    }



    private void
    update()
    {
      updateValue
      (
        name,
        selected,
        ((DefaultListModel) selected.getModel()).toArray()
      );
    }

  } // TransferSelectedListener

} // SimpleForm
