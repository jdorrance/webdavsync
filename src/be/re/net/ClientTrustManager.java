package be.re.net;

import be.re.gui.form.SimpleFormDialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Map;
import java.util.ResourceBundle;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;



/**
 * @author Werner Donn\u00e9
 */

public class ClientTrustManager implements X509TrustManager

{

  private final static char[]		PASSWORD =
    {'e', '5', 'j', 'w', '3', 'q', 'r', '5'};

  private static X509TrustManager	defaultManager;
  private boolean			interactive;



  public
  ClientTrustManager(boolean interactive)
  {
    this.interactive = interactive;
  }



  private void
  addCertificateText(Node root, X509Certificate cert, ResourceBundle bundle)
  {
    addLine
    (
      root,
      bundle.getString("label_cert_serial") + ": " +
        cert.getSerialNumber().toString()
    );

    addLine
    (
      root,
      bundle.getString("label_cert_issuer") + ": " +
        cert.getIssuerX500Principal().getName()
    );

    addLine
    (
      root,
      bundle.getString("label_cert_subject") + ": " +
        cert.getSubjectX500Principal().getName()
    );

    DateFormat	format = DateFormat.getDateInstance(DateFormat.SHORT);

    addLine
    (
      root,
      bundle.getString("label_cert_validity") + ": " +
        format.format(cert.getNotBefore()) + ", " +
        format.format(cert.getNotAfter())
    );
  }



  private void
  addLine(Node root, String line)
  {
    Node	div =
      be.re.xml.Util.
        selectFirstChild(root, "http://www.w3.org/1999/xhtml", "div");
    Node	table =
      be.re.xml.Util.
        selectFirstChild(div, "http://www.w3.org/1999/xhtml", "table");

    div.
      insertBefore
      (
        root.getOwnerDocument().
          createElementNS("http://www.w3.org/1999/xhtml", "p"),
        table
      ).appendChild(root.getOwnerDocument().createTextNode(line));
  }



  private boolean
  askUser(X509Certificate[] chain, boolean interactive)
  {
    try
    {
      if (verifyCertificate(chain[0]))
      {
        return true;
      }

      if (!interactive)
      {
        return false;
      }

      ResourceBundle	bundle = ResourceBundle.getBundle("be.re.net.res.Res");
      Document		form =
        be.re.xml.Util.getDocumentBuilder(null, false).parse
        (
          ClientTrustManager.class.
            getResource("res/accept_certificate_form.xml").toString()
        );

      addCertificateText(form.getDocumentElement(), chain[0], bundle);

      Map	fields =
        new SimpleFormDialog
        (
          bundle.getString("title_accept_certificate"),
          form,
          bundle
        ).getFields();

      if (fields == null)
      {
        return false;
      }

      if
      (
        fields.get("permanent") != null &&
          ((Boolean) ((Object[]) fields.get("permanent"))[0]).booleanValue()
      )
      {
        saveCertificate(chain[0]);
      }

      return true;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public void
  checkClientTrusted(X509Certificate[] chain, String authType)
    throws CertificateException
  {
    getDefaultManager().checkClientTrusted(chain, authType);
  }



  public void
  checkServerTrusted(X509Certificate[] chain, String authType)
    throws CertificateException
  {
    try
    {
      getDefaultManager().checkServerTrusted(chain, authType);
    }

    catch (CertificateException e)
    {
      if (!askUser(chain, interactive))
      {
        throw e;
      }
    }
  }



  public X509Certificate[]
  getAcceptedIssuers()
  {
    return
      defaultManager != null ?
        defaultManager.getAcceptedIssuers() : new X509Certificate[0];
  }



  private static X509TrustManager
  getDefaultManager() throws CertificateException
  {
    if (defaultManager != null)
    {
      return defaultManager;
    }

    try
    {
      TrustManagerFactory	factory =
        TrustManagerFactory.
          getInstance(TrustManagerFactory.getDefaultAlgorithm());

      factory.init((KeyStore) null);

      TrustManager[]	managers = factory.getTrustManagers();

      for (int i = 0; i < managers.length && defaultManager == null; ++i)
      {
        if (managers[i] instanceof X509TrustManager)
        {
          defaultManager = (X509TrustManager) managers[i];
        }
      }
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    if (defaultManager == null)
    {
      throw new CertificateException("No trust manager");
    }

    return defaultManager;
  }



  private static File
  getStorage(String filename)
  {
    File	directory =
      new File
      (
        new File(new File(System.getProperty("user.home"), ".be"), "re"),
        "net"
      );

    if (!directory.exists())
    {
      directory.mkdirs();
    }

    return new File(directory, filename);
  }



  private static synchronized void
  saveCertificate(X509Certificate cert) throws Exception
  {
    File	store = getStorage("TrustStore");
    File	tmpStore = getStorage("TrustStore.tmp");

    try
    {
      FileInputStream	in = store.exists() ? new FileInputStream(store) : null;
      OutputStream	out = new FileOutputStream(tmpStore);

      try
      {
        KeyStore	trustStore = KeyStore.getInstance("JKS");

        trustStore.load(in != null ? in : null, PASSWORD);
        trustStore.setCertificateEntry(cert.getSerialNumber().toString(), cert);
        trustStore.store(out, PASSWORD);
      }

      finally
      {
        out.close();

        if (in != null)
        {
          in.close();
        }
      }

      store.delete(); // Otherwise the rename will fail on Windows XP.
      tmpStore.renameTo(store);
    }

    catch (Exception e)
    {
      tmpStore.delete();

      throw e;
    }
  }



  private static boolean
  verifyCertificate(X509Certificate cert) throws Exception
  {
    File	store = getStorage("TrustStore");

    if (store.exists())
    {
      FileInputStream	in = new FileInputStream(store);

      try
      {
        KeyStore	trustStore = KeyStore.getInstance("JKS");

        trustStore.load(in, PASSWORD);

        return
          cert.equals
          (
            trustStore.getCertificate(cert.getSerialNumber().toString())
          );
      }

      finally
      {
        in.close();
      }
    }

    return false;
  }

} // ClientTrustManager
