package be.re.gui.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;



/**
 * This writer will pop up a window in which the written material is shown.
 * @author Werner Donn\u00e9
 */

public class InteractiveWriter extends Writer

{

  private StringBuilder				current = new StringBuilder();
  private boolean				errorMode = false;
  private JFrame				frame;
  private static Map<String,InteractiveWriter>	frames =
    new HashMap<String,InteractiveWriter>();
  private static Writer				log;
  private JButton				okButton = new JButton();
  private JScrollPane				pane;
  private HTMLPane				textArea =
    new HTMLPane
    (
      InteractiveWriter.class.getResource("res/interactive_writer.html"),
      InteractiveWriter.class.getResource("res/interactive_writer.css")
    )
    {
      protected void
      processKeyEvent(KeyEvent e)
      {
        if
        (
          e.getID() == KeyEvent.KEY_PRESSED	&&
          e.getKeyCode() == KeyEvent.VK_ENTER
        )
        {
          cleanUpFrame();
        }
        else
        {
          super.processKeyEvent(e);
        }
      }
    };



  private
  InteractiveWriter(String title)
  {
    this
    (
      new Frame(title, false)
      {
        public Dimension
        getPreferredSize()
        {
          return new Dimension(500, 300);
        }
      }
    );
  }



  private
  InteractiveWriter(JFrame frame)
  {
    this.frame = frame;
    textArea.setEditable(false);
    pane = new JScrollPane(textArea);
    pane.setBorder(null);

    AbstractAction	action =
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          cleanUpFrame();
        }
      };
    JPanel		buttonPanel = new JPanel();

    okButton.setEnabled(false);
    okButton.setActionCommand("ok");
    okButton.setText(Util.getResource("ok"));
    okButton.setDefaultCapable(true);
    buttonPanel.add(okButton);
    frame.getRootPane().setDefaultButton(okButton);
    okButton.addActionListener(action);

    frame.getRootPane().
      getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put
      (
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        "close"
      );

    frame.getRootPane().getActionMap().put("close", action);

    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(pane, BorderLayout.CENTER);
    frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

    frame.addWindowListener
    (
      new WindowAdapter()
      {
        public void
        windowClosed(WindowEvent e)
        {
          removeFrame(InteractiveWriter.this.frame);
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
          InteractiveWriter.this.frame.setVisible(true);
          goToLastLine();
        }
      }
    );
  }



  private void
  cleanUpFrame()
  {
    if (removeFrame(frame))
    {
      frame.dispose();
    }
  }



  public void
  close() throws IOException
  {
    okButton.setEnabled(true);
  }



  public void
  flush() throws IOException
  {
    if (log != null)
    {
      log.flush();
    }
  }



  private void
  goToLastLine()
  {
    try
    {
      Rectangle	view =
        textArea.modelToView(textArea.getDocument().getLength() - 1);

      if (view != null)
      {
        pane.getViewport().scrollRectToVisible(view);
      }
    }

    catch (BadLocationException e)
    {
      throw new RuntimeException(e.getMessage());
    }
  }



  public static boolean
  isOpen(String title)
  {
    return frames.get(title) != null;
  }



  public static InteractiveWriter
  open(String title)
  {
    return open(title, true);
  }



  public static synchronized InteractiveWriter
  open(String title, boolean log)
  {
    InteractiveWriter	result = frames.get(title);

    if (result == null)
    {
      result = new InteractiveWriter(title);
      frames.put(title, result);
    }

    setLog(log);

    return result;
  }



  public void
  raise()
  {
    frame.toFront();
  }



  public static void
  raise(String title)
  {
    InteractiveWriter	writer = frames.get(title);

    if (writer != null)
    {
      writer.raise();
    }
  }



  private static synchronized boolean
  removeFrame(JFrame frame)
  {
    return frames.remove(frame.getTitle()) != null;
  }



  public void
  setErrorMode(boolean value)
  {
    errorMode = value;
  }



  private static void
  setLog(boolean enable)
  {
    try
    {
      log =
        enable ?
          new OutputStreamWriter
          (
            new FileOutputStream
            (
              new File
              (
                be.re.util.Util.getPackageStorage(InteractiveWriter.class),
                "InteractiveWriter.log"
              ),
              true
            ),
            "UTF-8"
          ) : null;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public void
  write(char[] cbuf) throws IOException
  {
    write(cbuf, 0, cbuf.length);
  }



  public void
  write(char[] cbuf, int off, int len) throws IOException
  {
    write(new String(cbuf, off, len));
  }



  public void
  write(int c) throws IOException
  {
    write(new char[]{(char) c});
  }



  public void
  write(String str) throws IOException
  {
    int	index = str.indexOf(System.getProperty("line.separator"));

    if (index == -1)
    {
      current.append(str);
    }
    else
    {
      current.append(str.substring(0, index));
      write();

      int	rest = index + System.getProperty("line.separator").length();

      if (rest < str.length())
      {
        write(str.substring(rest));
      }
    }
  }



  public void
  write(String str, int off, int len) throws IOException
  {
    write(str.substring(off, off + len));
  }



  private void
  write() throws IOException
  {
    writeLog(current.toString());

    try
    {
      ((HTMLDocument) textArea.getDocument()).insertBeforeEnd
      (
        ((HTMLDocument) textArea.getDocument()).getElement("body"),
        (errorMode ? "<div class='error'>" : "<div>") + current.toString() +
          "</div>"
      );

      SwingUtilities.invokeLater
      (
        new Runnable()
        {
          public void
          run()
          {
            goToLastLine();
          }
        }
      );
    }

    catch (BadLocationException e)
    {
      throw new IOException(e.getMessage());
    }

    current.setLength(0);
  }



  private static synchronized void
  writeLog(String str) throws IOException
  {
    if (log != null)
    {
      log.write(be.re.util.Util.createTimestamp(System.currentTimeMillis()));
      log.write(' ');
      log.write(str);
      log.write(System.getProperty("line.separator"));
      log.flush();
    }
  }

} // InteractiveWriter
