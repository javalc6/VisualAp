/*
Version 1.0, 30-12-2007, First release
Version 1.1, 03-02-2008, added <version> field in components, added sorting of jars
Version 1.2, 06-01-2010, fixed minor compilation warnings with JDK 1.6, beans are now stored in sub-directory visualap of user home
Version 1.3, 29-11-2025, ClassPathHacker replaced by DynamicJarLoader for compatibility with java 9 and later releases

IMPORTANT NOTICE, please read:

This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE,
please read the enclosed file license.txt or http://www.gnu.org/licenses/licenses.html

Note that this software is freeware and it is not designed, licensed or intended
for use in mission critical, life support and military purposes.

The use of this software is at the risk of the user.
*/

/*
The LoadBeans loads all the available JavaBeans in the directory <beans>

javalc6

*/

package visualap;
import java.awt.event.*;
import javax.swing.JPanel;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.*;
import java.util.Collections;


class LoadBeans extends ArrayList<BeanDelegate> {
//    private transient HashMap<String, BeanDelegate> delegates = new HashMap<String, BeanDelegate>();

    public LoadBeans load(String beansDir) {
		ArrayList<String> jarNames = getJarNames(beansDir);	
		Collections.sort(jarNames);
		String name = null;
		try {
			ClassLoader classLoader = DynamicJarLoader.createLoaderForJars(jarNames);
			
			for (int i = 0; i < jarNames.size(); i++) {
				name = jarNames.get(i);
				ArrayList<String> beanNames = getBeansName(name);
				if (beanNames.size() != 0)	{	
					for (int k = 0; k < beanNames.size(); k++)	{
						String beanName = beanNames.get(k);
						try {
							boolean ignore = false;
							int remove_j = -1;
							BeanDelegate bd = new BeanDelegate(beanName, classLoader);
							for (int j = 0; j < this.size(); j++)
								if (get(j).name.equals(beanName)) {
									System.out.println(beanName+" duplicate detected");
									if (checkVersion(bd.version, get(j).version))
										ignore = true;
									else remove_j = j;
								}
							if (remove_j != -1)
								remove(remove_j);
							if (!ignore)
								add(bd);
						}
						catch (BeanException ex) {
		// this exception can be ignored
							ErrorPrinter.printInfo(beanName+" caused BeanException in " + name + " : "+ex.getMessage());
						}
					}
				}
			}
		} catch (Exception ex) {
			ErrorPrinter.printInfo(name == null ? "DynamicJarLoader failure" : name + ": jar load failed");
			ErrorPrinter.dump(ex, VisualAp.getUniqueID());
		}
		return this;
	}


// checkVersion returns true only if ver2 is greater than ver
    public boolean checkVersion(String ver, String ver2) {
		int min;
		if (ver.length() > ver2.length())
			min = ver2.length();
		else min = ver.length();
		for (int i=0; i<min; i++) {
			if (ver.charAt(i) < ver2.charAt(i))
				return true;
			else if (ver.charAt(i) == ver2.charAt(i))
					continue;
				else break;
		}
		return false;
    }



    public static ArrayList<String> getJarNames(String beansDir) {
		File jars = new File(beansDir);

		if (! jars.isDirectory()) {
			ErrorPrinter.printInfo(jars+" is not a directory!!");
		}

		ArrayList<String> result = new ArrayList<>();
		String names[];
		names = jars.list((f, name) -> name.toLowerCase().endsWith(".jar"));
		if (names != null)
            for (String name : names) {
                result.add(jars.getPath() + File.separatorChar + name);
            }

		return result;
    }


	public ArrayList<String> getBeansName(String filename) throws IOException {
        JarFile jarfile;
        Manifest mf;
		ArrayList<String> result = new ArrayList<>();

		jarfile = new JarFile(filename);
		mf = jarfile.getManifest();

        Attributes attribs = mf.getMainAttributes();
        if (attribs != null)  {
            // Determine if this is a java bean.
            String isJavaBean = attribs.getValue(new Attributes.Name("Java-Bean"));

            if (isJavaBean != null && isJavaBean.equalsIgnoreCase("True"))  {
                String classname = attribs.getValue(new Attributes.Name("Name"));

				if (classname.endsWith(".class"))  {
					classname = classname.substring(0, classname.length() - 6);
				} else if (classname.endsWith(".ser")) {
					// Must deserialize the class.
					classname = classname.substring(0, classname.length() - 4);
				}
				result.add(classname.replace('/', '.'));
            }
        }

        for (String beanName : mf.getEntries().keySet()) {
            attribs = mf.getAttributes(beanName);

            if (attribs != null) {
                String isJavaBean = attribs.getValue(new Attributes.Name("Java-Bean"));

                if (isJavaBean != null && isJavaBean.equalsIgnoreCase("True")) {
                    if (beanName.endsWith(".class")) {
                        beanName = beanName.substring(0, beanName.length() - 6);
                    } else if (beanName.endsWith(".ser")) {
                        // Must deserialize the class.
                        beanName = beanName.substring(0, beanName.length() - 4);
                    }
                    result.add(beanName.replace('/', '.'));
                }
            }
        }
		return result;
	}
}
