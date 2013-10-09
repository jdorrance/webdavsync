package be.re.webdav.cmd;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.sql.DriverManager;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;



public class SyncAntTask extends Task

{

  private String	direction;
  private File		directory;
  private String	excludes = "";
  private String	hiddenFolderName;
  private boolean	noRecursion;
  private boolean	rename;
  private int		renameDepth = 10;
  private boolean	recur;
  private URL		url;



  public void
  execute() throws BuildException
  {
    if (direction == null)
    {
      throw new BuildException("The attribute \"direction\" is required.");
    }

    if
    (
      !direction.equals("down")			&&
      !direction.equals("up")			&&
      !direction.equals("bidirectional")
    )
    {
      throw
        new BuildException
        (
          "The attribute \"direction\" should have the value \"down\", " +
            "\"up\" or \"bidirectional\"."
        );
    }

    if (directory == null)
    {
      throw new BuildException("The attribute \"directory\" is required.");
    }

    if (url == null)
    {
      throw new BuildException("The attribute \"url\" is required.");
    }

    if (!url.toString().endsWith("/"))
    {
      throw
        new BuildException
        (
          "The URL should denote a collection (end with a \"/\")."
        );
    }

    try
    {
      System.setProperty("java.awt.headless", "true"); // For Derby.
      DriverManager.registerDriver(new EmbeddedDriver());

      Sync	sync = new Sync();

      sync.setExclusions(be.re.util.Util.split(excludes, " ,"));
      sync.addListener(new Sync.Reporter(new LogOutputStream(this, 0)));
      sync.setInteractive(false);
      sync.setDaemonMode(true);
      sync.setRecursive(recur);
      sync.setLocal(directory);
      sync.setRemote(url);
      sync.setHiddenFolderName(hiddenFolderName);
      sync.setRenameFiles(rename);
      sync.setRenameDepth(renameDepth);
      sync.setNoRecursion(noRecursion);

      sync.setDirection
      (
        direction.equals("down") ?
          Sync.Direction.DOWN :
          (
            direction.equals("up") ?
              Sync.Direction.UP : Sync.Direction.BIDIRECTIONAL
          )
      );

      sync.run();
    }

    catch (Exception e)
    {
      throw new BuildException(e);
    }
  }



  public void
  setDirection(String value)
  {
    direction = value;
  }



  public void
  setDirectory(File value)
  {
    directory = value;
  }



  public void
  setExcludes(String value)
  {
    excludes = value;
  }



  public void
  setHiddenFolderName(String value)
  {
    hiddenFolderName = value;
  }



  public void
  setNoRecursion(boolean value)
  {
    noRecursion = value;
  }



  public void
  setRecur(boolean value)
  {
    recur = value;
  }



  public void
  setRename(boolean value)
  {
    rename = value;
  }



  public void
  setRenameDepth(int value)
  {
    renameDepth = value;
  }



  public void
  setUrl(URL value)
  {
    url = value;
  }

} // SyncAntTask
