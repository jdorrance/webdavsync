package be.re.gui.form;

import be.re.gui.util.Frame;
import be.re.gui.util.HelpButton;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.w3c.dom.Document;



/**
 * A dialog wrapper around the SimpleForm.
 * @author Werner Donn\u00e9
 */

public class SimpleFormDialog extends JDialog

{

  private JPanel 	buttonPanel = new JPanel();
  private Runnable	cancel = null;
  private Action	cancelAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        if (cancel != null)
        {
          cancel.run();
        }

        dispose();
      }
    };
  private static JFrame	defaultOwner;
  private Map		fields = null;
  private SimpleForm	form;
  private Action	okAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        if (processFields != null)
        {
          process();
        }
        else
        {
          fields = form.getFields();
          dispose();
        }
      }
    };
  private ProcessFields	processFields = null;



  public
  SimpleFormDialog(String title, Document form, ResourceBundle bundle)
  {
    this(title, form, false, bundle, null);
  }



  public
  SimpleFormDialog
  (
    String		title,
    Document		form,
    ResourceBundle	bundle,
    URL			help
  )
  {
    this(title, form, false, bundle, help);
  }



  public
  SimpleFormDialog
  (
    String		title,
    Document		form,
    ResourceBundle	bundle,
    URL			help,
    JMenuBar		helpMenuBar
  )
  {
    this(title, form, false, bundle, help, helpMenuBar);
  }



  public
  SimpleFormDialog
  (
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle
  )
  {
    this(title, form, notifyOnly, bundle, null);
  }



  public
  SimpleFormDialog
  (
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle,
    URL			help
  )
  {
    this
    (
      defaultOwner != null ? defaultOwner : new JFrame(),
      title,
      form,
      notifyOnly,
      bundle,
      help
    );
  }



  public
  SimpleFormDialog
  (
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle,
    URL			help,
    JMenuBar		helpMenuBar
  )
  {
    this
    (
      defaultOwner != null ? defaultOwner : new JFrame(),
      title,
      form,
      notifyOnly,
      bundle,
      help,
      helpMenuBar
    );
  }



  public
  SimpleFormDialog
  (
    JFrame		frame,
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle
  )
  {
    this(frame, title, form, notifyOnly, bundle, null);
  }



  public
  SimpleFormDialog
  (
    JFrame		frame,
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle,
    URL			help
  )
  {
    this(frame, title, form, notifyOnly, bundle, help, null);
  }



  public
  SimpleFormDialog
  (
    JFrame		frame,
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle,
    URL			help,
    JMenuBar		helpMenuBar
  )
  {
    this(frame, title, form, notifyOnly, bundle, help, helpMenuBar, null);
  }



  public
  SimpleFormDialog
  (
    JFrame		frame,
    String		title,
    Document		form,
    boolean		notifyOnly,
    ResourceBundle	bundle,
    URL			help,
    JMenuBar		helpMenuBar,
    JComponent		extra
  )
  {
    super(frame, title);
    setResizable(false);
    this.form = new SimpleForm(form, bundle);
    init(notifyOnly, help, helpMenuBar, extra);
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



  public static JFrame
  getDefaultOwner()
  {
    return defaultOwner;
  }



  /**
   * Returns a map of field names to <code>Object[]</code> values.
   */

  public Map
  getFields()
  {
    fields = null;
    processFields = null;
    openDialog();

    return fields;
  }



  public SimpleForm
  getForm()
  {
    return form;
  }



  private void
  init(boolean notifyOnly, URL help, JMenuBar helpMenuBar, JComponent extra)
  {
    JPanel 	controlPanel = new JPanel();
    JButton 	okButton = new JButton();

    okButton.setActionCommand("ok");
    okButton.setText(be.re.gui.util.Util.getResource("ok"));
    okButton.addActionListener(okAction);
    okButton.setDefaultCapable(true);

    getRootPane().setDefaultButton(okButton);
    getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
      put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    getRootPane().getActionMap().put("cancel", cancelAction);

    getContentPane().
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(form);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 5)));
    getContentPane().add(controlPanel);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 10)));

    controlPanel.setLayout(new OverlayLayout(controlPanel));
    controlPanel.add(buttonPanel);

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(okButton);

    if (!notifyOnly)
    {
      JButton 	cancelButton = new JButton();

      cancelButton.setActionCommand("cancel");
      cancelButton.setText(be.re.gui.util.Util.getResource("cancel"));
      cancelButton.addActionListener(cancelAction);

      buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      buttonPanel.add(cancelButton);
    }

    if (help != null)
    {
      JPanel	helpPanel = new JPanel();

      helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.X_AXIS));
      helpPanel.add(Box.createHorizontalGlue());
      helpPanel.add(new HelpButton(help, helpMenuBar));
      helpPanel.setOpaque(false);
      helpPanel.setBorder(new EmptyBorder(0, 0, 0, 10));
      controlPanel.add(helpPanel);
    }

    if (extra != null)
    {
      JPanel	extraPanel = new JPanel();

      extraPanel.setLayout(new BoxLayout(extraPanel, BoxLayout.X_AXIS));
      extraPanel.add(extra);
      extraPanel.add(Box.createHorizontalGlue());
      extraPanel.setOpaque(false);
      extraPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
      controlPanel.add(extraPanel);
    }
  }



  public void
  open()
  {
    buttonPanel.setVisible(false); // Live mode, so there is nothing to commit.

    open
    (
      new ProcessFields()
      {
        public boolean
        process(Map fields)
        {
          return true;
        }
      }
    );
  }



  public void
  open(ProcessFields processFields)
  {
    open(processFields, null);
  }



  public void
  open(ProcessFields processFields, Runnable cancel)
  {
    this.processFields = processFields;
    this.cancel = cancel;
    fields = null;
    openDialog();
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



  private void
  process()
  {
    new Thread
    (
      new Runnable()
      {
        public void
        run()
        {
          cancelAction.setEnabled(false);
          okAction.setEnabled(false);

          if (processFields.process(form.getFields()))
          {
            SwingUtilities.invokeLater
            (
              new Runnable()
              {
                public void
                run()
                {
                  dispose();

                  synchronized (SimpleFormDialog.this)
                  {
                    SimpleFormDialog.this.notifyAll();
                  }
                }
              }
            );
          }
          else
          {
            cancelAction.setEnabled(true);
            okAction.setEnabled(true);
          }
        }
      }
    ).start();
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



  public interface ProcessFields

  {

    public boolean	process	(Map fields);

  } // ProcessFields

} // SimpleFormDialog
