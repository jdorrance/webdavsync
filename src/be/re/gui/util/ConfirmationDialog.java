package be.re.gui.util;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;



/**
 * A dialog wrapper around a table.
 * @author Werner Donn\u00e9
 */

public class ConfirmationDialog extends JDialog

{

  private Action	cancelAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        result = false;
        dispose();
      }
    };
  private static JFrame	defaultOwner;
  private Action	okAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        result = true;
        dispose();
      }
    };
  boolean		result = false;



  public
  ConfirmationDialog(String title)
  {
    this(title, false);
  }



  public
  ConfirmationDialog(String title, boolean modal)
  {
    super(defaultOwner != null ? defaultOwner : new JFrame(), title, modal);
    init();
    pack();

    setLocation
    (
      (int) (getToolkit().getScreenSize().getWidth() / 2) -
        (int) (getSize().getWidth() / 2),
      (int) (getToolkit().getScreenSize().getHeight() / 2) -
        (int) (getSize().getHeight() / 2)
    );
  }



  public void
  addNotify()
  {
    super.addNotify();

    if (Frame.getDefaultIcon() != null)
    {
      be.re.gui.util.Util.getFrame(this).
        setIconImage(Frame.getDefaultIcon().getImage());
    }
  }



  public boolean
  confirm()
  {
    openDialog();

    return result;
  }



  public static JFrame
  getDefaultOwner()
  {
    return defaultOwner;
  }



  private void
  init()
  {
    JPanel 	buttonPanel = new JPanel();
    JButton 	cancelButton = new JButton();
    JButton 	okButton = new JButton();

    cancelButton.setActionCommand("cancel");
    cancelButton.setText(be.re.gui.util.Util.getResource("cancel"));
    cancelButton.addActionListener(cancelAction);
    cancelButton.setDefaultCapable(true);

    okButton.setActionCommand("ok");
    okButton.setText(be.re.gui.util.Util.getResource("ok"));
    okButton.addActionListener(okAction);

    getRootPane().setDefaultButton(cancelButton);
    getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
      put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    getRootPane().getActionMap().put("cancel", cancelAction);

    getContentPane().
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

    getContentPane().add
    (
      Util.addPadding
      (
        new JLabel(Util.getResource("msg_confirmation")),
        20,
        100,
        20,
        100
      )
    );

    getContentPane().add(Box.createRigidArea(new Dimension(0, 5)));
    getContentPane().add(buttonPanel);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 10)));

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(okButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    buttonPanel.add(cancelButton);
  }



  private void
  openDialog()
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
  }



  public void
  removeNotify()
  {
    super.removeNotify();

    synchronized (this)
    {
      notifyAll();
    }
  }



  public static void
  setDefaultOwner(JFrame frame)
  {
    defaultOwner = frame;
  }

} // ConfirmationDialog
