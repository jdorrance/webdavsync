package be.re.gui.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;



/**
 * @author Werner Donn\u00e9
 */

public class YesOrNo extends JDialog

{

  private JTextArea	area;
  private static JFrame	defaultOwner;
  private Action	noAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        dispose();
        yes = false;

        synchronized (YesOrNo.this)
        {
          YesOrNo.this.notifyAll();
        }
      }
    };
  private boolean	yes = false;
  private Action	yesAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        dispose();
        yes = true;

        synchronized (YesOrNo.this)
        {
          YesOrNo.this.notifyAll();
        }
      }
    };



  public
  YesOrNo(String question)
  {
    this(question, -1, -1, null, null, false);
  }



  public
  YesOrNo(String question, boolean modal)
  {
    this(question, -1, -1, null, null, modal);
  }



  public
  YesOrNo(String question, int rows, int columns)
  {
    this(question, rows, columns, null, null, false);
  }



  public
  YesOrNo(String question, int rows, int columns, boolean modal)
  {
    this(question, rows, columns, null, null, modal);
  }



  public
  YesOrNo
  (
    String	question,
    int		rows,
    int		columns,
    String	yesLabel,
    String	noLabel
  )
  {
    this(question, rows, columns, yesLabel, noLabel, false);
  }



  public
  YesOrNo
  (
    String	question,
    int		rows,
    int		columns,
    String	yesLabel,
    String	noLabel,
    boolean	modal
  )
  {
    super(defaultOwner != null ? defaultOwner : new JFrame(), modal);
    init(question, rows, columns, yesLabel, noLabel);
    place();
  }



  public
  YesOrNo(JComponent component, String yesLabel, String noLabel, boolean modal)
  {
    super(defaultOwner != null ? defaultOwner : new JFrame(), modal);
    init(component, yesLabel, noLabel);
    place();
  }



  public void
  addNotify()
  {
    super.addNotify();

    if (area != null)
    {
      area.setBackground(getContentPane().getBackground());
    }

    if (Frame.getDefaultIcon() != null)
    {
      Util.getFrame(this).setIconImage(Frame.getDefaultIcon().getImage());
    }
  }



  public boolean
  ask()
  {
    if (!isModal())
    {
      SwingUtilities.invokeLater
      (
        new Runnable()
        {
          public void
          run()
          {
            setVisible(true);
          }
        }
      );

      synchronized (this)
      {
        try
        {
          wait();

          return yes;
        }

        catch (InterruptedException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
    else
    {
      setVisible(true);
    }

    return yes;
  }



  public static JFrame
  getDefaultOwner()
  {
    return defaultOwner;
  }



  private void
  init(String question, int rows, int columns, String yesLabel, String noLabel)
  {
    area =
      rows != -1 && columns != -1 ?
        new JTextArea(question, rows, columns) : new JTextArea(question);
    area.setEditable(false);
    area.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    if (rows != -1 && columns != -1)
    {
      area.setLineWrap(true);
    }

    init
    (
      rows != -1 && columns != -1 ? new JScrollPane(area) : area,
      yesLabel,
      noLabel
    );
  }



  private void
  init(JComponent component, String yesLabel, String noLabel)
  {
    JPanel 	buttonPanel = new JPanel();
    JButton 	noButton = new JButton();
    JButton 	yesButton = new JButton();

    noButton.setActionCommand("no");
    noButton.setText(noLabel != null ? noLabel : Util.getResource("no"));
    noButton.addActionListener(noAction);

    yesButton.setActionCommand("yes");
    yesButton.setText(yesLabel != null ? yesLabel : Util.getResource("yes"));
    yesButton.addActionListener(yesAction);
    yesButton.setDefaultCapable(true);

    getRootPane().setDefaultButton(yesButton);
    getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
      put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "no");
    getRootPane().getActionMap().put("no", noAction);

    getContentPane().
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

    component.setFocusable(false);
    getContentPane().add(component);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 5)));
    getContentPane().add(buttonPanel);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 10)));

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(Box.createRigidArea(new Dimension(25, 0)));
    buttonPanel.add(yesButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    buttonPanel.add(noButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(25, 0)));
  }



  private void
  place()
  {
    pack();
 
    setLocation
    (
      (int) (getToolkit().getScreenSize().getWidth() / 2) -
        (int) (getSize().getWidth() / 2),
      (int) (getToolkit().getScreenSize().getHeight() / 2) -
        (int) (getSize().getHeight() / 2)
    );
  }



  protected void
  processWindowEvent(final WindowEvent e)
  {
    super.processWindowEvent(e);

    if (e.getID() == WindowEvent.WINDOW_CLOSING)
    {
      synchronized (this)
      {
        notifyAll();
      }
    }
  }



  public static void
  setDefaultOwner(JFrame frame)
  {
    defaultOwner = frame;
  }

} // YesOrNo
