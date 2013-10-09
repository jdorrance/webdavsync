package be.re.net;

import be.re.gui.form.Option;
import be.re.gui.form.SimpleForm;
import be.re.gui.form.SimpleFormDialog;
import be.re.gui.util.AuthenticateDialog;
import be.re.io.StreamConnector;
import be.re.util.PBE;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509KeyManager;
import javax.swing.SwingUtilities;
import org.w3c.dom.Document;



/**
 * An interactive key manager for SSL client authentication.
 * @author Werner Donn\u00e9
 */

public class ClientKeyManager implements X509KeyManager

{

  private static Map		aliases = new HashMap();
  private boolean		interactive;
  private static Map		keyPasswords = new HashMap();
  private static KeyStore	keyStore;
  private static char[]		password;



  public
  ClientKeyManager(boolean interactive)
  {
    this.interactive = interactive;
  }



  private static char[]
  askPassword(String title)
  {
    User			user = new BasicUser();
    final AuthenticateDialog	dialog =
      new AuthenticateDialog(title, user, true);

    SwingUtilities.invokeLater
    (
      new Runnable()
      {
        public void
        run()
        {
          dialog.setVisible(true);
        }
      }
    );

    synchronized (dialog)
    {
      try
      {
        dialog.wait();
      }

      catch (InterruptedException e)
      {
        throw new RuntimeException(e);
      }
    }

    if (user.getPassword() == null)
    {
      throw new AbortException(Util.getResource("msg_aborted"));
    }

    return user.getPassword().toCharArray();
  }



  public String
  chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
  {
    String	result = (String) aliases.get(socket.getRemoteSocketAddress());

    if (result != null)
    {
      return result;
    }

    List	aliasList = new ArrayList();
    List	certificates = new ArrayList();

    getClientAliases(aliasList, certificates, issuers);

    result =
      aliasList.size() == 1 ?
        (String) aliasList.get(0) :
        (
          aliasList.size() > 1 ?
            chooseCertificate(aliasList, certificates) : null
        );

/*
    if (result == null)
    {
      result = importCertificate();
    }
*/

    if (result == null)
    {
      throw new AbortException(Util.getResource("msg_aborted"));
    }

    aliases.put(socket.getRemoteSocketAddress(), result);

    return result;
  }



  private static String
  chooseCertificate(List aliases, List certificates)
  {
    String[]	names = new String[certificates.size()];

    for (int i = 0; i < names.length; ++i)
    {
      names[i] =
        (String) aliases.get(i) + ": " +
          ((X509Certificate) certificates.get(i)).getSubjectDN().getName();
    }

    ResourceBundle	bundle = ResourceBundle.getBundle("be.re.net.res.Res");
    Document		form = null;

    try
    {
      form =
        be.re.xml.Util.getDocumentBuilder(null, false).parse
        (
          ClientKeyManager.class.
            getResource("res/choose_certificate.xml").toString()
        );
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    Option[]	options = be.re.gui.form.Util.createOptions(names);

    be.re.gui.form.Util.populateSelection(form, "certificate", options);

    Map	fields =
      new SimpleFormDialog
      (
        bundle.getString("title_choose_certificate"),
        form,
        bundle
      ).getFields();

    if
    (
      fields == null						||
      fields.get("certificate") == null				||
      ((Object[]) fields.get("certificate")).length != 1
    )
    {
      return null;
    }

    String	selected = (String) ((Object[]) fields.get("certificate"))[0];

    for (int i = 0; i < names.length; ++i)
    {
      if (names[i].equals(selected))
      {
        return (String) aliases.get(i);
      }
    }

    return null;
  }



  public String
  chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
  {
    return null;
  }



  private static byte[]
  decode(String[] lines, int start) throws Exception
  {
    int	length = 0;

    for (int i = start; i < lines.length - 1; ++i)
    {
      length += lines[i].length();
    }

    int		position = 0;
    byte[]	result = new byte[length];

    for (int i = start; i < lines.length - 1; ++i)
    {
      byte[]	bytes = lines[i].getBytes("ASCII");

      System.arraycopy(bytes, 0, result, position, bytes.length);
      position += bytes.length;
    }

    return be.re.util.Base64.decode(result);
  }



  private static byte[]
  decrypt(byte[] b, String[] decryptInfo, String password) throws Exception
  {
    String	algorithm =
      decryptInfo[0].substring(0, decryptInfo[0].lastIndexOf('-'));
    String	mode =
      decryptInfo[0].substring(decryptInfo[0].lastIndexOf('-') + 1);

    algorithm =
      "DES".equals(algorithm) ?
        "DES" :
        (
          "DES-EDE".equals(algorithm) || "DES-EDE3".equals(algorithm) ?
            "DESede" : algorithm
        );

    Cipher	cipher =
      Cipher.getInstance(algorithm + "/" + mode + "/NoPadding");
      //Cipher.getInstance(algorithm + "/" + mode + "/PKCS5Padding");
    Key		key =
      SecretKeyFactory.getInstance(algorithm).generateSecret
      (
        "DES".equals(algorithm) ?
          (KeySpec) new DESKeySpec(padKey(password.getBytes("ASCII"), 8)) :
          (
            "DESede".equals(algorithm) ?
              (KeySpec)
                new DESedeKeySpec(padKey(password.getBytes("ASCII"), 24)) :
              (KeySpec) new PBEKeySpec(password.toCharArray())
          )
      );

    if (decryptInfo.length == 2)
    {
      cipher.init(Cipher.DECRYPT_MODE, key, getIV(algorithm, decryptInfo[1]));
    }
    else
    {
      cipher.init(Cipher.DECRYPT_MODE, key);
    }

    return cipher.doFinal(b);
  }



  /**
   * Returns <code>true</code> if the connection was set-up using the key
   * manager.
   */

  public boolean
  failed(Socket socket)
  {
    if (socket instanceof SSLSocket)
    {
      ((SSLSocket) socket).getSession().invalidate();

      return aliases.remove(socket.getRemoteSocketAddress()) != null;
    }

    return false;
  }



  public X509Certificate[]
  getCertificateChain(String alias)
  {
    try
    {
      Certificate[]	result = getKeyStore().getCertificateChain(alias);

      if
      (
        result == null				||
        result.length == 0			||
        !(result[0] instanceof X509Certificate)
      )
      {
        Certificate	certificate = getKeyStore().getCertificate(alias);

        return
          certificate instanceof X509Certificate ?
            new X509Certificate[]{(X509Certificate) certificate} : null;
      }

      X509Certificate[]	copy = new X509Certificate[result.length];

      System.arraycopy(result, 0, copy, 0, result.length);

      return copy;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public String[]
  getClientAliases(String keyType, Principal[] issuers)
  {
    List	aliasList = new ArrayList();
    List	certificates = new ArrayList();

    getClientAliases(aliasList, certificates, issuers);

    return (String[]) aliasList.toArray(new String[0]);
  }



  private void
  getClientAliases(List aliasList, List certificates, Principal[] issuers)
  {
    try
    {
      for (Enumeration i = getKeyStore().aliases(); i.hasMoreElements();)
      {
        String		alias = (String) i.nextElement();
        Certificate	certificate = getKeyStore().getCertificate(alias);

        if
        (
          certificate instanceof X509Certificate	&&
          getKeyStore().isKeyEntry(alias)
        )
        {
          for (int j = 0; j < issuers.length; ++j)
          {
            try
            {
              if
              (
                new X500Principal(issuers[j].getName()).equals
                (
                  new X500Principal
                  (
                    ((X509Certificate) certificate).getIssuerDN().getName()
                  )
                )
              )
              {
                aliasList.add(alias);
                certificates.add(certificate);
              }
            }

            catch (IllegalArgumentException e)
            {
              // Not a distinguished name.
            }
          }
        }
      }
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private static AlgorithmParameters
  getIV(String algorithm, String s) throws Exception
  {
    byte[]	bytes = new byte[s.length() / 2];

    for (int i = 0; i < s.length(); i += 2)
    {
      bytes[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
    }

    AlgorithmParameters	result = AlgorithmParameters.getInstance(algorithm);

    result.init(new IvParameterSpec(bytes));

    return result;
  }



  private KeyStore
  getKeyStore()
  {
    if (keyStore != null)
    {
      return keyStore;
    }

    try
    {
      File	file =
        new File(new File(System.getProperty("user.home")), ".keystore");
      KeyStore	store = KeyStore.getInstance(KeyStore.getDefaultType());

      store.
        load(file.exists() ? new FileInputStream(file) : null, getPassword());

      keyStore = store;

      return store;
    }

    catch (Exception e)
    {
      password = null;

      throw new RuntimeException(e);
    }
  }



  private char[]
  getPassword()
  {
    if (password == null && interactive)
    {
      password = askPassword(Util.getResource("title_keystore"));
    }

    return password;
  }



  public PrivateKey
  getPrivateKey(String alias)
  {
    char[]	password = (char[]) keyPasswords.get(alias);

    if (password != null)
    {
      try
      {
        return (PrivateKey) getKeyStore().getKey(alias, password);
      }

      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }

    try
    {
      return tryPrivateKey(alias, getPassword()); // Try key store password.
    }

    catch (UnrecoverableKeyException e)
    {
      if (!interactive)
      {
        while
        (
          (
            password =
              askPassword(Util.getResource("title_key_password") + " " + alias)
          ) != null
        )
        {
          try
          {
            return tryPrivateKey(alias, password);
          }

          catch (UnrecoverableKeyException ex)
          {
          }
        }
      }
    }

    return null;
  }



  public String[]
  getServerAliases(String keyType, Principal[] issuers)
  {
    return null;
  }



  private static File
  getStorage()
  {
    return new File(new File(System.getProperty("user.home")), ".keystore");
  }



  private static String[]
  getValues(String value)
  {
    StringTokenizer	tokenizer = new StringTokenizer(value, ", ");
    String[]		result = new String[tokenizer.countTokens()];

    for (int i = 0; i < result.length; ++i)
    {
      result[i] = tokenizer.nextToken();
    }

    return result;
  }



  private String
  importCertificate()
  {
    try
    {
      ResourceBundle	bundle = ResourceBundle.getBundle("be.re.net.res.Res");
      Document		form =
        be.re.xml.Util.getDocumentBuilder(null, false).parse
        (
          ClientKeyManager.class.
            getResource("res/import_certificate_form.xml").toString()
        );
      final String[]	result = new String[1];

      new SimpleFormDialog
      (
        bundle.getString("title_import_certificate"),
        form,
        bundle
      ).open
      (
        new SimpleFormDialog.ProcessFields()
        {
          public boolean
          process(Map fields)
          {
            if
            (
              fields == null			||
              fields.get("alias") == null	||
              fields.get("certificate") == null	||
              fields.get("key") == null
            )
            {
              return true;
            }

            if
            (
              importCertificate
              (
                (String) ((Object[]) fields.get("alias"))[0],
                new File((String) ((Object[]) fields.get("certificate"))[0]),
                new File((String) ((Object[]) fields.get("key"))[0]),
                fields.get("password") != null &&
                  !"".equals((String) ((Object[]) fields.get("password"))[0]) ?
                  (String) ((Object[]) fields.get("password"))[0] : null
              )
            )
            {
              result[0] = (String) ((Object[]) fields.get("alias"))[0];

              return true;
            }

            return false;
          }
        }
      );

      return result[0];
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private boolean
  importCertificate(String alias, File certificate, File key, String password)
  {
    FileInputStream	in = null;

    try
    {
      in = new FileInputStream(certificate);

      getKeyStore().setKeyEntry
      (
        alias,
        KeyFactory.getInstance("RSA").
          generatePrivate(new PKCS8EncodedKeySpec(readKey(key, password))),
        getPassword(),
        (Certificate[])
          CertificateFactory.getInstance("X.509").generateCertificates(in).
            toArray(new Certificate[0])
      );

      saveKeyStore();

      return true;
    }

    catch (CertificateException e)
    {
      be.re.util.Util.printStackTrace(e);
      be.re.gui.util.Util.report(Util.getResource("msg_invalid_certificate"));

      return false;
    }

    catch (KeyStoreException e)
    {
      be.re.util.Util.printStackTrace(e);
      be.re.gui.util.Util.report(Util.getResource("msg_keystore_add"));

      return false;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    finally
    {
      if (in != null)
      {
        try
        {
          in.close();
        }

        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }



  private static byte[]
  padBytes(byte[] bytes, int modulo)
  {
    if (bytes.length % modulo == 0)
    {
      return bytes;
    }

    byte[]	result =
      new byte[bytes.length + (modulo - bytes.length % modulo)];

    System.arraycopy(bytes, 0, result, 0, bytes.length);
    Arrays.fill(result, bytes.length, result.length, (byte) 0);

    return result;
  }



  private static byte[]
  padKey(byte[] key, int size)
  {
    if (key.length == size)
    {
      return key;
    }

    byte[]	result = new byte[size];

    System.arraycopy(key, 0, result, 0, Math.min(size, key.length));
    Arrays.fill(result, Math.min(size, key.length), size, (byte) 0);

    return result;
  }



  private static byte[]
  readKey(File file, String password) throws Exception
  {
    String[]	lines = readLines(file);

    if
    (
      (
        !"-----BEGIN RSA PRIVATE KEY-----".equals(lines[0])	         &&
        !"-----BEGIN DSA PRIVATE KEY-----".equals(lines[0])
      )								         ||
      (
        !"-----END RSA PRIVATE KEY-----".equals(lines[lines.length - 1]) &&
        !"-----END DSA PRIVATE KEY-----".equals(lines[lines.length - 1])
      )
    )
    {
      return new byte[0];
    }

    String[]	decryptInfo = null;
    int		i;
    boolean	shouldDecrypt = false;

    for
    (
      i = 1;
      i < lines.length - 1 && !"".equals(lines[i]) &&
        lines[i].indexOf(':') != -1;
      ++i
    )
    {
      String	name = lines[i].substring(0, lines[i].indexOf(':')).trim();
      String[]	values =
        getValues(lines[i].substring(lines[i].indexOf(':') + 1).trim());

      if ("Proc-Type".equals(name))
      {
        if
        (
          values.length != 2			||
          !"4".equals(values[0])		||
          !"ENCRYPTED".equals(values[1])
        )
        {
          return new byte[0];
        }

        shouldDecrypt = true;
      }
      else
      {
        if ("DEK-Info".equals(name))
        {
          decryptInfo = values;
        }
      }
    }

    if
    (
      shouldDecrypt				&&
      (
        decryptInfo == null			||
        (
          decryptInfo.length != 2		&&
          decryptInfo.length != 1
        )					||
        (
          decryptInfo.length == 2		&&
          decryptInfo[1].length() % 2 != 0
        )
      )
    )
    {
      return new byte[0];
    }

    return
      shouldDecrypt ?
        decrypt(decode(lines, i), decryptInfo, password) : decode(lines, i);
  }



  private static byte[]
  readKey(File file) throws IOException
  {
    ByteArrayOutputStream	out = new ByteArrayOutputStream();

    StreamConnector.copy(new FileInputStream(file), out);

    return out.toByteArray();
  }



  private static String[]
  readLines(File file) throws IOException
  {
    BufferedReader	in =
      new BufferedReader
      (
        new InputStreamReader(new FileInputStream(file), "ASCII")
      );
    String		line;
    List		result = new ArrayList();

    while ((line = in.readLine()) != null)
    {
      result.add(line);
    }

    return (String[]) result.toArray(new String[0]);
  }



  private void
  saveKeyStore()
  {
    File	tmpStore = new File(getStorage().getAbsolutePath() + ".tmp");

    try
    {
      OutputStream	out = new FileOutputStream(tmpStore);

      getKeyStore().store(out, getPassword());
      out.close();
      tmpStore.renameTo(getStorage());
    }

    catch (Exception e)
    {
      tmpStore.delete();

      be.re.gui.util.Util.report(Util.getResource("msg_keystore_save"));
    }
  }



  private PrivateKey
  tryPrivateKey(String alias, char[] password) throws UnrecoverableKeyException
  {
    try
    {
      Key	result = getKeyStore().getKey(alias, password);

      if (result != null)
      {
        if (result instanceof PrivateKey)
        {
          keyPasswords.put(alias, password);

          return (PrivateKey) result;
        }
      }

      return null;
    }

    catch (UnrecoverableKeyException e)
    {
      throw e;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

} // ClientKeyManager
