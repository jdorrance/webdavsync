package be.re.gui.util;

import be.re.net.User;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;



/**
 * @author Werner Donn\u00e9
 */

public class AuthenticateDialog extends JDialog

{

  private boolean		accepted = false;
  private JButton 		cancelButton = new JButton();
  private boolean		cancelled = false;
  private static JFrame		defaultOwner;
  private JButton 		okButton = new JButton();
  private JLabel		passwordLabel = new JLabel();
  private JPasswordField	passwordField = new JPasswordField();
  private JCheckBox		remember = new JCheckBox();
  private User 			user;
  private JTextField 		usernameField = new JTextField();
  private JLabel		usernameLabel = new JLabel();

  private Action	cancelAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        cancel(true);
      }
    };

  private Action	okAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        user.setUsername(usernameField.getText());
        user.setPassword(new String(passwordField.getPassword()));
        accepted = true;
        dispose();

        synchronized (AuthenticateDialog.this)
        {
          AuthenticateDialog.this.notifyAll();
        }
      }
    };



  public
  AuthenticateDialog(String title, User user, boolean lockUsername)
  {
    this(title, user, lockUsername, false);
  }



  public
  AuthenticateDialog
  (
    String	title,
    User	user,
    boolean	lockUsername,
    boolean	enableRemember
  )
  {
    this
    (
      defaultOwner != null ? defaultOwner : new JFrame(),
      title,
      user,
      lockUsername,
      enableRemember
    );
  }



  public
  AuthenticateDialog
  (
    JFrame	frame,
    String	title,
    User	user,
    boolean	lockUsername,
    boolean	enableRemember
  )
  {
    this(frame, title, false, enableRemember);
    this.user = user;
    usernameField.setText(user.getUsername() != null ? user.getUsername() : "");

    if (lockUsername)
    {
      usernameField.setEnabled(false);
    }
  }



  public
  AuthenticateDialog(JFrame frame, String title, boolean modal)
  {
    this(frame, title, modal, false);
  }



  public
  AuthenticateDialog
  (
    JFrame	frame,
    String	title,
    boolean	modal,
    boolean	enableRemember
  )
  {
    super(frame, title, modal);

    try
    {
      init(enableRemember);
      pack();

      setLocation
      (
        (int) (getToolkit().getScreenSize().getWidth() / 2) -
          (int) (getSize().getWidth() / 2),
        (int) (getToolkit().getScreenSize().getHeight() / 2) -
          (int) (getSize().getHeight() / 2)
      );

      addWindowListener
      (
        new WindowAdapter()
        {
          public void
          windowClosing(WindowEvent e)
          {
            if (!cancelled && !accepted)
            {
              cancel(false);
            }
          }
        }
      );
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public
  AuthenticateDialog()
  {
    this(null, "", false, false);
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



  private void
  cancel(boolean closeWindow)
  {
    user.setUsername(null);
    user.setPassword(null);
    cancelled = true;

    if (closeWindow)
    {
      dispose();
    }

    synchronized (AuthenticateDialog.this)
    {
      AuthenticateDialog.this.notifyAll();
    }
  }



  private void
  createLayout(boolean enableRemember)
  {
    JPanel	buttonPanel = new JPanel();
    JPanel	mainPanel = new JPanel(new GridBagLayout());

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(okButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    buttonPanel.add(cancelButton);

    mainPanel.add
    (
      usernameLabel,
      new GridBagConstraints
      (
        0,
        0,
        1,
        1,
        0.0,
        0.0,
        GridBagConstraints.WEST,
        GridBagConstraints.NONE,
        new Insets(5, 15, 5, 5),
        0,
        0
      )
    );

    mainPanel.add
    (
      passwordLabel,
      new GridBagConstraints
      (
        0,
        1,
        1,
        1,
        0.0,
        0.0,
        GridBagConstraints.WEST,
        GridBagConstraints.NONE,
        new Insets(5, 15, 5, 5),
        0,
        0
      )
    );

    mainPanel.add
    (
      passwordField,
      new GridBagConstraints
      (
        1,
        1,
        1,
        1,
        1.0,
        1.0,
        GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL,
        new Insets(5, 5, 5, 15),
        0,
        0
      )
    );

    mainPanel.add
    (
      usernameField,
      new GridBagConstraints
      (
        1,
        0,
        1,
        1,
        1.0,
        1.0,
        GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL,
        new Insets(5, 5, 5, 15),
        0,
        0
      )
    );

    if (enableRemember)
    {
      mainPanel.add
      (
        remember,
        new GridBagConstraints
        (
          0,
          2,
          2,
          1,
          1.0,
          1.0,
          GridBagConstraints.CENTER,
          GridBagConstraints.NONE,
          new Insets(5, 5, 5, 5),
          0,
          0
        )
      );
    }

    getContentPane().
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(Box.createRigidArea(new Dimension(0, 10)));
    getContentPane().add(mainPanel);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 5)));
    getContentPane().add(buttonPanel); 
    getContentPane().add(Box.createRigidArea(new Dimension(0, 10)));
  }



  public static JFrame
  getDefaultOwner()
  {
    return defaultOwner;
  }



  private void
  init(boolean enableRemember) throws Exception
  {
    usernameLabel.setText(Util.getResource("username_label"));
    passwordLabel.setText(Util.getResource("password_label"));
    remember.setText(Util.getResource("remember_label"));

    passwordField.setPreferredSize
    (
      new Dimension(200, (int) passwordField.getPreferredSize().getHeight())
    );

    usernameField.setPreferredSize
    (
      new Dimension(200, (int) usernameField.getPreferredSize().getHeight())
    );

    cancelButton.setActionCommand("cancel");
    cancelButton.setText(Util.getResource("cancel"));
    cancelButton.addActionListener(cancelAction);

    okButton.setActionCommand("ok");
    okButton.setText(Util.getResource("ok"));
    okButton.addActionListener(okAction);
    okButton.setDefaultCapable(true);

    getRootPane().setDefaultButton(okButton);
    getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
      put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    getRootPane().getActionMap().put("cancel", cancelAction);

    createLayout(enableRemember);
  }



  public static void
  setDefaultOwner(JFrame frame)
  {
    defaultOwner = frame;
  }



  public boolean
  shouldRemember()
  {
    return remember.isSelected();
  }

} // AuthenticateDialog
