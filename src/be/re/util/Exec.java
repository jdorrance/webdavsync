package be.re.util;

import be.re.io.StreamConnector;
import java.io.IOException;



/**
 * The class executes a program in the hosting OS.
 */

public class Exec

{

  /**
   * Executes a program in the hosting OS. Stdin, stdout and stderr are
   * redirected.
   * @param args the command line to execute.
   * @exception IOException A problem during redirection.
   * @exception InterruptedException Problem during thread manipulation.
   */

  public static void
  main(String[] args) throws IOException, InterruptedException
  {
    Process	process = Runtime.getRuntime().exec(args);

    StreamConnector	out =
      new StreamConnector(process.getInputStream(), System.out, false, false);
    StreamConnector	err =
      new StreamConnector(process.getErrorStream(), System.err, false, false);

    // Commented out code will be reinserted when bug 4103109 is fixed.
    //  StreamConnector	in =
    //    new StreamConnector(System.in, process.getOutputStream(), 1);
    // in.join();
    out.join();
    err.join();

    Util.waitFor(process);
  }

} // Exec
