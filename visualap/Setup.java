/*
Version 1.0, 12-03-2011, Setup program for initial installation

IMPORTANT NOTICE, please read:

This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE,
please read the enclosed file license.txt or http://www.gnu.org/licenses/licenses.html

Note that this software is freeware and it is not designed, licensed or intended
for use in mission critical, life support and military purposes.

The use of this software is at the risk of the user.

Usage: java -jar visualap.setup.jar

Note: Java 1.6 or greater is required

javalc6

*/

package visualap;
import java.io.*;
import java.util.*;
import java.util.prefs.*;
import java.util.jar.*;

public class Setup {
	static Preferences prefs = Preferences.userNodeForPackage(Setup.class);
	static final String version = "1.2.2";

	static boolean checkExt(String s, String ext) {
		int i = s.lastIndexOf('.');
		if (i == -1)  return (false);
		return (s.substring(i+1).toLowerCase().matches(ext.toLowerCase()));
	}

	public static int copy(File source, File destination) throws IOException {
		if (source.getCanonicalPath().equals(destination.getCanonicalPath()))
			return 0; // nothing to copy...
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(destination);

        byte[] buffer = new byte[2048];
        int n, count = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }


	public static void extractFromJAR(JarFile jar, JarEntry entry, String dest) throws IOException {
		InputStream in = new BufferedInputStream(jar.getInputStream(entry));
		File output = new File(dest, entry.getName());
		if ((output.getParentFile() != null) && !output.getParentFile().exists())
			output.getParentFile().mkdirs(); // (re-)create destination dir if not exists

		OutputStream out = new BufferedOutputStream(new FileOutputStream(output));
		byte[] buffer = new byte[2048];
		for (;;)  {
			int nBytes = in.read(buffer);
			if (nBytes <= 0) break;
			out.write(buffer, 0, nBytes);
		}
		out.flush();
		out.close();
		in.close();
	}

    private static String getJarFileName() {
      String urlStr = Setup.class.getResource("Setup.class").toString();
      int from = "jar:file:".length();
      int to = urlStr.indexOf("!/");
	  if (to == -1)
		  return null;
      return urlStr.substring(from, to);
    }

	public static String getDataPath() {
		String datapath = prefs.get("dataPath", null);
		if (datapath == null) {
			datapath = System.getProperty("user.home")+File.separatorChar+"visualap";
			new File(datapath+File.separatorChar+"beans").mkdirs();
			prefs.put("dataPath", datapath);
		}
		return datapath;
	}

	public static void main(String[] args) {
		String jarname = getJarFileName();
		if (jarname == null) {
			System.err.println("Installation failed: unknown jarfilename"); // never happen
			System.exit(0);
		}
		String datapath = getDataPath();
		if (!prefs.get("version", "1.1").equals(version)) // default version "1.1" is used for obsolete version, it includes also 1.0 version
			System.out.print("Re-installation in progress...");
		else System.out.print("Upgrade in progress...");

// copy files in home directory (beans, vas, images and sounds)
		try	{
			JarFile f = new JarFile(jarname);			
			String dataPath = prefs.get("dataPath", null); 
			Enumeration<JarEntry> entries = f.entries();
			while(entries.hasMoreElements()) {
				JarEntry entry = (JarEntry) entries.nextElement();
				String entryname = entry.getName();
				if (entry.isDirectory()) {
					new File(entryname).mkdirs();
				} else if (entryname.startsWith("beans/")) {
					extractFromJAR(f, entry, dataPath);
				} else if ((!entryname.contains("/")) && checkExt(entryname, "vas|jpg|png|gif|wav|mp3")) {
					extractFromJAR(f, entry, dataPath);
				} else {
					extractFromJAR(f, entry, "."); // extracts other files in the current path
				}
				System.out.print(".");
			}
//			f.close();
		} catch (FileNotFoundException ex) {
			System.err.println("Installation failed, exception: "+ ex);
			System.err.println("Make sure that files can be created in the current directory, check write permission.");
			System.exit(0);
		} catch (IOException ex) {
			System.err.println("Installation failed, exception: "+ ex);
			System.err.println("Problem occured reading "+jarname);
			System.exit(0);
		}

		prefs.put("version", version);
		System.out.println("Installation completed");
	}

}
