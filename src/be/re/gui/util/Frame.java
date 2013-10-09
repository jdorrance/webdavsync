package be.re.gui.util;

import be.re.util.BasicClassLoader;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.Timer;



public class Frame extends JFrame

{

  private List			blockCloseListeners = new ArrayList();
  private boolean		closing = false;
  private String		coordinatesName;
  private Rectangle		defaultBounds;
  private static ImageIcon	defaultIcon =
    new ImageIcon(Frame.class.getResource("res/rezw.png"));
  private boolean		internal = false;
  private boolean		mainWindow;
  private static Set<Frame>	openedFrames = new HashSet<Frame>();
  private boolean		saveBounds;
  private Timer			timer =
    new Timer
    (
      1000,
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          setTitle(title);
        }
      }
    );
  private String		title;



  public
  Frame()
  {
    this(null, null, false, null, true);
  }



  public
  Frame(boolean saveBounds)
  {
    this(null, null, false, null, saveBounds);
  }



  public
  Frame(String title, boolean mainWindow)
  {
    this(title, title, mainWindow, null, true);
  }



  public
  Frame(String title, boolean mainWindow, boolean saveBounds)
  {
    this(title, title, mainWindow, null, saveBounds);
  }



  public
  Frame(String title, boolean mainWindow, Rectangle defaultBounds)
  {
    this(title, title, mainWindow, defaultBounds, true);
  }



  public
  Frame
  (
    String	title,
    boolean	mainWindow,
    Rectangle	defaultBounds,
    boolean	saveBounds
  )
  {
    this(title, title, mainWindow, defaultBounds, saveBounds);
  }



  public
  Frame(String title, String coordinatesName, boolean mainWindow)
  {
    this(title, coordinatesName, mainWindow, null, true);
  }



  public
  Frame
  (
    String	title,
    String	coordinatesName,
    boolean	mainWindow,
    boolean	saveBounds
  )
  {
    this(title, coordinatesName, mainWindow, null, saveBounds);
  }



  public
  Frame
  (
    String	title,
    String	coordinatesName,
    boolean	mainWindow,
    Rectangle	defaultBounds
  )
  {
    this(title, coordinatesName, mainWindow, defaultBounds, true);
  }



  public
  Frame
  (
    String	title,
    String	coordinatesName,
    boolean	mainWindow,
    Rectangle	defaultBounds,
    boolean	saveBounds
  )
  {
    if (defaultIcon != null)
    {
      setIconImage(defaultIcon.getImage());
    }

    if (title != null)
    {
      setTitle(title);
    }

    this.coordinatesName = coordinatesName;
    this.mainWindow = mainWindow;
    this.defaultBounds = defaultBounds;
    this.saveBounds = saveBounds;
    timer.setRepeats(false);

    addComponentListener
    (
      new ComponentAdapter()
      {
        public void
        componentMoved(ComponentEvent e)
        {
          timer.start();

          setInternalTitle
          (
            String.valueOf(getX()) + "," + String.valueOf(getY())
          );
        }

        public void
        componentResized(ComponentEvent e)
        {
          timer.start();

          setInternalTitle
          (
            String.valueOf(getWidth()) + "x" + String.valueOf(getHeight())
          );
        }
      }
    );

    if (be.re.util.Util.isMac())
    {
      ((JComponent) getContentPane()).
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
        put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.META_MASK), "close");

      ((JComponent) getContentPane()).getActionMap().put
      (
        "close",
        new AbstractAction()
        {
          public void
          actionPerformed(ActionEvent e)
          {
            dispatchEvent
            (
              new WindowEvent(Frame.this, WindowEvent.WINDOW_CLOSING)
            );
          }
        }
      );
    }
  }



  public void
  addBlockCloseListener(BlockCloseListener listener)
  {
    blockCloseListeners.add(listener);
  }



  public void
  addNotify()
  {
    super.addNotify();

    if
    (
      saveBounds			&&
      getCoordinatesName() != null	&&
      !getCoordinatesName().equals("")
    )
    {
      File	file = getStorage(getCoordinatesName());

      if (file.exists())
      {
        InputStream	in = null;

        try
        {
          Properties	coordinates = new Properties();
          Dimension	screen = Toolkit.getDefaultToolkit().getScreenSize();

          in = new FileInputStream(file);
          coordinates.load(in);

          int	height = Integer.parseInt(coordinates.getProperty("height"));
          int	width = Integer.parseInt(coordinates.getProperty("width"));
          int	x = Integer.parseInt(coordinates.getProperty("x"));
          int	y = Integer.parseInt(coordinates.getProperty("y"));

          if (x < 0)
          {
            x = 0;
          }
          else
          {
            if (x > (int) screen.getWidth())
            {
              x = Math.max(0, (int) screen.getWidth() - width);
            }
          }

          if (y < 0)
          {
            y = 0;
          }
          else
          {
            if (y > (int) screen.getHeight())
            {
              y = Math.max(0, (int) screen.getHeight() - height);
            }
          }

          setLocation(x, y);

          setSize
          (
            Math.min(width, (int) screen.getWidth() - x),
            Math.min(height, (int) screen.getHeight() - y)
          );
        }

        catch (IOException e)
        {
          // Don't prevent the window from opening.

          setBounds(getInitialBounds());
        }

        finally
        {
          if (in != null)
          {
            try
            {
              in.close();
            }

            catch (IOException e)
            {
              // Don't prevent the window from opening.
            }
          }
        }
      }
      else
      {
        setBounds(getInitialBounds());
      }
    }
    else
    {
      setBounds(getInitialBounds());
    }
  }



  private boolean
  clientsAgree()
  {
    List		copy = (List) ((ArrayList) blockCloseListeners).clone();
    BlockCloseEvent	e = new BlockCloseEvent();

    for (Iterator i = copy.iterator(); i.hasNext();)
    {
      ((BlockCloseListener) i.next()).closing(e);
    }

    return !e.shouldBlock;
  }



  private void
  close()
  {
    dispose();

    if
    (
      mainWindow							&&
      (
        getClass().getClassLoader() == null				||
        !(getClass().getClassLoader() instanceof BasicClassLoader)
      )
    )
    {
      try
      {
        //System.exit(0);
      }

      catch (Throwable ex)
      {
        // Only attempt.
      }
    }
  }



  public void
  dispose()
  {
    openedFrames.remove(this);

    if (saveBounds)
    {
      saveCoordinates();
    }

    super.dispose();
  }



  private String
  getCoordinatesName()
  {
    return
      coordinatesName == null || coordinatesName.equals("") ?
        getTitle() : coordinatesName;
  }



  public static ImageIcon
  getDefaultIcon()
  {
    return defaultIcon;
  }



  public static Frame
  getFrame(String title)
  {
    for (Frame frame: openedFrames)
    {
      if (title.equals(frame.getTitle()))
      {
        return frame;
      }
    }

    return null;
  }



  private Rectangle
  getInitialBounds()
  {
    return
      defaultBounds != null ?
        defaultBounds :
        new Rectangle
        (
          new Point
          (
            (int) (getToolkit().getScreenSize().getWidth() / 2) -
              (int) (getPreferredSize().getWidth() / 2),
            (int) (getToolkit().getScreenSize().getHeight() / 2) -
              (int) (getPreferredSize().getHeight() / 2)
          ),
          getPreferredSize()
        );
  }



  public static Frame[]
  getOpenedFrames()
  {
    return openedFrames.toArray(new Frame[0]);
  }



  private static File
  getStorage(String filename)
  {
    File	directory =
      new File
      (
        new File
        (
          new File(new File(System.getProperty("user.home"), ".be"), "re"),
          "gui"
        ),
        "frame"
      );

    if (!directory.exists())
    {
      directory.mkdirs();
    }

    return new File(directory, filename);
  }



  public String
  getTitle()
  {
    String	s = super.getTitle();
    int		index = s.indexOf(" -- ");

    return index != -1 ? s.substring(0, index) : s;
  }



  protected void
  processWindowEvent(final WindowEvent e)
  {
    if (!closing && e.getID() == WindowEvent.WINDOW_CLOSING)
    {
      if (blockCloseListeners.size() > 0)
      {
        new Thread
        (
          new Runnable()
          {
            public void
            run()
            {
              if (clientsAgree())
              {
                closing = true;
                dispatchEvent(e);
                close();
              }
            }
          }
        ).start();
      }
      else
      {
        close();
      }
    }
    else
    {
      if (e.getID() == WindowEvent.WINDOW_OPENED)
      {
        openedFrames.add(this);
      }

      super.processWindowEvent(e);
    }
  }



  public void
  removeBlockCloseListener(BlockCloseListener listener)
  {
    blockCloseListeners.remove(listener);
  }



  private void
  saveCoordinates()
  {
    if (getCoordinatesName() == null || getCoordinatesName().equals(""))
    {
      return;
    }

    File	saveFile = null;

    try
    {
      saveFile =
        be.re.io.Util.createTempFile
        (
          "frame",
          getCoordinatesName(),
          getStorage(getCoordinatesName()).getParentFile()
        );

      PrintStream	out = new PrintStream(new FileOutputStream(saveFile));

      out.println("x=" + String.valueOf(getX()));
      out.println("y=" + String.valueOf(getY()));
      out.println("width=" + String.valueOf(getWidth()));
      out.println("height=" + String.valueOf(getHeight()));
      out.close();
      getStorage(getCoordinatesName()).delete();
      saveFile.renameTo(getStorage(getCoordinatesName()));
      saveFile = null;
    }

    catch (Throwable ex)
    {
      if (saveFile != null)
      {
        saveFile.delete();
      }

      // Don't prevent closing if this fails.
    }
  }



  public static void
  setDefaultIcon(ImageIcon icon)
  {
    defaultIcon = icon;
  }



  private void
  setInternalTitle(String s)
  {
    internal = true;
    super.setTitle(title + " -- " + s);
    internal = false;
  }



  public void
  setTitle(String s)
  {
    if (!internal)
    {
      title = s;
    }

    super.setTitle(s);
  }



  public class BlockCloseEvent

  {

    private boolean	shouldBlock;



    public void
    block()
    {
      shouldBlock = true;
    }



    public Frame
    getSource()
    {
      return Frame.this;
    }

  } // BlockCloseEvent



  public interface BlockCloseListener

  {

    public void	closing	(BlockCloseEvent e);

  } // BlockCloseListener

} // Frame
