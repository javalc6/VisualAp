/*
Version 1.0, 30-12-2007, First release
Version 1.1, 03-02-2008, added Check Latest Version feature, added automatic downloading of new/modified components, 
new method updateComponents(), prepared for MDI support, corrected a fault related to text insertion, other minor updates
Version 1.2, 06-01-2010, fixed minor compilation warnings with JDK 1.6, removed class BareBonesBrowserLaunch as not needed in JDK 1.6,
new cursor when selecting a component from the toolbox, arrow drawing towards input pins, Properties replaced by Preferences,
data files stored in sub-directory visualap of user home (windows vista and windows 7 compatibility)
Version 1.2.1, 13-03-2010, modified handling of properties, replaced obsolete com.sun.image.codec.jpeg package and added support for gif and png images, dnd support, enhanced setup


IMPORTANT NOTICE, please read:

This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE,
please read the enclosed file license.txt or http://www.gnu.org/licenses/licenses.html

Note that this software is freeware and it is not designed, licensed or intended
for use in mission critical, life support and military purposes.

The use of this software is at the risk of the user.
*/

/*
VisualAp

This program creates a graphic window with a menu and scrollable panel
From the menu it is possible to open a file dialog, print dialog, help window, preference dialog, an about box and exit
It is possible to create, move, edit rectangular objects (box)
Popup menu is supported, mouse cursor changes during dragging
It is possible to use keyboard accelerators like Ctrl+C, Ctrl+X, Ctrl+V, Ctrl+S, Ctrl+N, Ctrl+O, Ctrl+P, F1...
The program supports selections of multiple objects by dragging the mouse cursor. Copy and paste is supported.
Read/Write an xml file that contains the objects (box)

Usage: java visualap.VisualAp  [-fast] [-run] [-report] [-uniqueID] [-help] <filename>

-fast       fast startup
-run        automatic run
-report     print a report about available beans
-uniqueID   print uniqueID
-help       this help

Note: Java 1.6 or greater is required to run VisualAp

VisualAp is based on the use of javabeans conforming to the readme.txt recommendations

javalc6

*/ 
package visualap;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*; 
import javax.swing.event.*;
import javax.swing.text.html.*;
import java.awt.print.*;
import java.io.*;
import java.util.*;
import parser.*;
//  MDI Support
import javax.swing.JInternalFrame;
import javax.swing.JDesktopPane;
import java.net.URL;
import java.util.prefs.*;
import graph.*;
// Drag and drop Support
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

public class VisualAp extends JFrame implements DropTargetListener {


	static final String WindowTitle = VisualAp.getAppName();
	static final String ABOUTMSG = WindowTitle+"\nVersion "+Setup.version+"\n13-03-2011\njavalc6";
	static final String defaultFileName = "noname.vas";
	static final String hostingWebServer = "http://visualap.sourceforge.net/";
	static GPanel activePanel;
	static String fname=defaultFileName;
	static ImageIcon largeicon = new ImageIcon(VisualAp.class.getResource("logo64.png"));
	static ImageIcon icon = new ImageIcon(VisualAp.class.getResource("logo16.gif"));
	static JFileChooser fc = null;
	static Preferences prefs = Preferences.userNodeForPackage(VisualAp.class);
	DialogPref DialogPref;
	HelpWindow hWindow = new HelpWindow(545+70, 20);

	static final double z_arrow_length = 8.0; // arrow length in pixels
	static final double z_arrow_angle = 0.5; // arrow angle in radiants

	private static ToolBox toolBox;
	private static boolean autorun;

	private static boolean wo_fc;
	private static Thread thread_fc;

	ArrayList<BeanDelegate> beans;
    JDesktopPane desktop;
	Point savedPoint = new Point(0,0);
	ArrayList<Node> copyL = new ArrayList<Node>();

	public VisualAp(String filename) {
		super(WindowTitle);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				quit();
			}
		});
		String datapath = prefs.get("dataPath", null); 
		beans = new LoadBeans().load(datapath+File.separatorChar+"beans");
		insert = newItem("Insert");
		fname = filename;

		desktop = new JDesktopPane(); //MDI Support
		toolBox = new ToolBox(beans, new InsertBean());

		setupMenuBar();
		JScrollPane scrollToolbox = new JScrollPane(toolBox);

		//Provide minimum sizes for the two components in the split pane
		scrollToolbox.setMinimumSize(new Dimension(100, 50));
		//Create a split pane with the two scroll panes in it
		JSplitPane topPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
								   desktop, scrollToolbox);
//		topPane.setOneTouchExpandable(true);
		topPane.setDividerLocation(400);
		topPane.setResizeWeight(1.0);
		setBounds(70, 20, 545, 500);
//		getContentPane().add(topPane);
		setContentPane(topPane);

		new DropTarget(desktop, this);

		DialogPref = new DialogPref(this);
		DialogPref.pack();

		setIconImage(icon.getImage());

		if (wo_fc) {
		// workaround for JFileChooser too slow to start-up: start a thread to create a filechooser
			thread_fc = new Thread() {
				// This method is called when the thread runs
				public void run() {
					try {
						create_fc();
					} catch(java.util.concurrent.RejectedExecutionException e){ 
		//				ErrorPrinter.printInfo("RejectedExecutionException");	
						quit();
					}
				}
			};
			thread_fc.start();
		} else create_fc();

		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
//				getToolkit().sync();
				if (fname != null)
					load(fname);
				else fname = defaultFileName;

				if (autorun)
                    SwingUtilities.invokeLater((Runnable) () -> {
                        try	{
                            StringBuffer error = new StringBuffer();
                            Engine engine = new Engine(VisualAp.this);
                            engine.runDialog(
                                new Check(activePanel.nodeL, activePanel.EdgeL).checkSystem(), error);
    //						engine.join();
                            if (engine.cancel)
                                JOptionPane.showMessageDialog(VisualAp.this, "Run cancelled", "VisualAp", JOptionPane.INFORMATION_MESSAGE, icon);
                            else if (error.length() == 0)
                                    System.exit(0); // JOptionPane.showMessageDialog(VisualAp.this, "Run successful", "VisualAp", JOptionPane.INFORMATION_MESSAGE, icon);
                                else JOptionPane.showMessageDialog(VisualAp.this, error,"Error",JOptionPane.ERROR_MESSAGE);
                        } catch (CheckException ex) {
                            JOptionPane.showMessageDialog(VisualAp.this, ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                        }
                    });
			}
		});
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
		setTitle(WindowTitle);
		setVisible(true);
/*		if (firstrun)
			hWindow.setPage(VisualAp.class.getResource("helpfile5.html"));

		if (beans.size() == 0)
			JOptionPane.showMessageDialog(VisualAp.this, "No beans found: please reinstall the application",
					"No beans found!",JOptionPane.ERROR_MESSAGE);
*/
	//	setAlwaysOnTop(true);

	}

	protected void load(String fname) {
		HashSet<String> updatel = new HashSet<>();
		try	{
			if (activePanel == null) createFrame(new GPanel(beans, VisualAp.this, new File(fname), updatel));
			else activePanel.readXML(new File(fname), updatel);
			activePanel.setTitle(fname);
		}	catch (VersionException ex)	{
			updateComponents(updatel);
			fname=defaultFileName;
		}	catch (IOException ex)	{
			ErrorPrinter.printInfo(ex.toString());
			JOptionPane.showMessageDialog(VisualAp.this, ex.getMessage(),
				"Open failed",JOptionPane.ERROR_MESSAGE);
			fname=defaultFileName;
//						quit();
		}	catch (Exception ex)	{
			ErrorPrinter.printInfo(ex.toString());
			ErrorPrinter.dump(ex, getUniqueID());
			quit();
		}
	}

//MDI support: Create a new internal frame.
    protected void createFrame(GPanel gpanel) {
		activePanel = gpanel;
		GFrame frame = new GFrame(activePanel);
        frame.setVisible(true);
        desktop.add(frame);
		save.setEnabled(true);
		saveas.setEnabled(true);
		print.setEnabled(true);
		check.setEnabled(true);
		run.setEnabled(true);
		editprops.setEnabled(true);
		insert.setEnabled(true);
		try {
			frame.setMaximum(true);
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) { 
			System.err.println("VisualAp.createFrame(): PropertyVetoException");
		}
    }

	public void dragEnter(DropTargetDragEvent dtde) {}

	public void dragExit(DropTargetEvent dte) {}

	public void dragOver(DropTargetDragEvent dtde) {}

	public void dropActionChanged(DropTargetDragEvent dtde) {}

	public void drop(DropTargetDropEvent dtde) {
		try {
		  Transferable tr = dtde.getTransferable();
		  DataFlavor[] flavors = tr.getTransferDataFlavors();
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorJavaFileListType()) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List list = (java.util.List) tr.getTransferData(flavor);
/* for debug purpose
			  for (int j = 0; j < list.size(); j++)
				System.out.println(list.get(j) + "\n");
*/

// take only the last filename (in case of multi-drop)
                    load(list.get(list.size() - 1).toString());
                    dtde.dropComplete(true);
                    return;
                }
            }
		  dtde.rejectDrop();
		} catch (Exception ex) {
			dtde.rejectDrop();
			ErrorPrinter.dump(ex, getUniqueID());
		}
	}


static int openFrameCount = 0;

class GFrame extends JInternalFrame  {
// MDI Support
    static final int xOffset = 30, yOffset = 30;
	final GPanel gpanel; // gpanel <--> gframe

	public GFrame (GPanel gpanel) {
		super();
/*        super("Document #" + (++openFrameCount), 
              true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable
// MDI Support
//        setSize(300,300);
//        setLocation(xOffset*openFrameCount, yOffset*openFrameCount);
*/
		JScrollPane scrollgPane = new JScrollPane(activePanel);
		setContentPane(scrollgPane);
//		scrollgPane.setMinimumSize(new Dimension(100, 50));
		this.gpanel = gpanel;
		gpanel.frame = this;
	}
}


// insert a bean: originated by ToolBox.java
private class InsertBean implements callback {
    public void doInsert(Class bean, String beanName, Cursor cursor) {
		if (activePanel == null) {
			createFrame(new GPanel(beans, VisualAp.this));
			fname = defaultFileName;
			activePanel.setTitle(fname);
	    }
		activePanel.doInsert(bean, beanName, cursor);
    }
	public boolean isNull() {
		return (activePanel == null) || (activePanel.insertBean == null);
	};
}

	void updateComponents(HashSet<String> updatel) {
		if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(VisualAp.this,
			"Download new and updated components?", "Open failed",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)) {

			Cursor savedCursor = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			HashSet<String> downloadfiles = new HashSet<>();
			WebFetch fetch = new WebFetch();
			String datapath = prefs.get("dataPath", null); 
			try {
				for (String clazz : updatel) {
					String fn = fetch.fetchURL(hostingWebServer+"download.php?component="+
						clazz,"<filename>");
					if (downloadfiles.contains(fn))
						continue;
					System.out.println("Downloading: "+fn);
					fetch.fetchFile(hostingWebServer+"download.php?filename="+fn,new File(datapath+File.separatorChar+"beans"+File.separatorChar+fn));
					downloadfiles.add(fn);
				}
				JOptionPane.showMessageDialog(VisualAp.this, 
					"Download completed\nExiting: restart VisualAp", "VisualAp", JOptionPane.INFORMATION_MESSAGE, icon);
				System.exit(0);
			}
			catch (java.net.ConnectException cex) {
				JOptionPane.showMessageDialog(VisualAp.this, "Connection cannot be setup\nPlease read help","Error",JOptionPane.ERROR_MESSAGE);
				hWindow.setPage(VisualAp.class.getResource("helpfile5a.html"));
			}
			catch (IOException ioex) {
				JOptionPane.showMessageDialog(VisualAp.this, "Technical problems\n"+ioex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			}
			setCursor(savedCursor);
		}
	}
	

	void create_fc() {
		fc = new JFileChooser(prefs.get("dataPath", null));
		fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public String getDescription() { return "VAS files"; }
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith (".vas") || f.isDirectory ();
			}
		});
	}

	void wait_thread_fc() {
// workaround for JFileChooser: wait until it is created
		if (thread_fc.isAlive())	{
			try {
				Cursor savedCursor = getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				thread_fc.join();
				setCursor(savedCursor);
			} catch (InterruptedException ex) { 
				ErrorPrinter.printInfo("InterruptedException");
				ErrorPrinter.dump(ex, getUniqueID());
			}
		}
	}

// voci del menu di primo livello
// File Edit Help
//

	JMenu editMenu;
	JMenuItem edit = editItem("Properties...");
	JMenuItem copy = copyItem("Copy");
	JMenuItem cut = cutItem("Cut");
	JMenuItem unbind = unbindItem("Unbind");
	JMenuItem paste = pasteItem("Paste");
	JMenuItem insert;
	JMenuItem save = new JMenuItem("Save");
	JMenuItem saveas = new JMenuItem("Save As...");
	JMenuItem print = new JMenuItem("Print...");
	JMenuItem check = new JMenuItem("Check");
	JMenuItem run = new JMenuItem("Run");
	JMenuItem editprops = new JMenuItem("Properties...");
	JMenuItem prefer = new JMenuItem("Preferences...");

	void setupMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(buildFileMenu());
		editMenu = menuBar.add(buildEditMenu());
		menuBar.add(buildSystemMenu());
		menuBar.add(buildToolsMenu());
		menuBar.add(buildHelpMenu());

	// editMenu � un menu variabile, quindi gestito dinamicamente via listener
		editMenu.addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {
			}
			public void menuDeselected(MenuEvent e) {
			}
			public void menuSelected(MenuEvent e) {
				if (activePanel != null) {
					edit.setEnabled(activePanel.selection.size() == 1);
					cut.setEnabled(activePanel.selection.size() != 0);
					unbind.setEnabled(activePanel.selection.size() != 0);
					copy.setEnabled(activePanel.selection.size() != 0);
					paste.setEnabled(copyL.size() != 0);
				} else {
					edit.setEnabled(false);
					cut.setEnabled(false);
					unbind.setEnabled(false);
					copy.setEnabled(false);
					paste.setEnabled(false);
				}
			}
		});	

		setJMenuBar(menuBar);	
	}


    protected JMenu buildFileMenu() {
		JMenu file = new JMenu("File");
		JMenuItem newWin = new JMenuItem("New");
		JMenuItem open = new JMenuItem("Open...");
		save.setEnabled(false);
		saveas.setEnabled(false);
		print.setEnabled(false);
		JMenuItem quit = new JMenuItem("Quit");

// Begin "New"
		newWin.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				if (checkAndSave()) return;
				if (activePanel == null) 
					createFrame(new GPanel(beans, VisualAp.this));
				activePanel.clear();
				fname = defaultFileName;
				activePanel.setTitle(fname);
		   }});
		newWin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
// End "New"

// Begin "Open"
		open.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				if (checkAndSave()) return;
// workaround for JFileChooser: wait until it is created
				if (wo_fc)
					wait_thread_fc();
				int returnVal = fc.showOpenDialog(VisualAp.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
					HashSet<String> updatel = new HashSet<>();
					try	{ 	
						File file = fc.getSelectedFile();
						if (activePanel == null) createFrame(new GPanel(beans, VisualAp.this, file, updatel));
						else activePanel.readXML(file, updatel);
						fname = file.getPath();
						activePanel.setTitle(file.getPath());
					}	catch (VersionException ex)	{
						updateComponents(updatel);
					}	catch (IOException ex) {
						JOptionPane.showMessageDialog(VisualAp.this, ex.getMessage(),"Open failed",JOptionPane.ERROR_MESSAGE);
					}   catch (Exception ex) {
						//ex.printStackTrace();
						ErrorPrinter.dump(ex, getUniqueID());
						ErrorPrinter.showDialog(VisualAp.this, ex);
					}
                }
		   }});
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
// End "Open"

// Begin "Save"
		save.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				   try {
					   activePanel.writeXML(new File(fname));
				   }
				   catch (IOException ex)
				   {    
						ErrorPrinter.showDialog(VisualAp.this, ex);
				   }
		   }});
// End "Save"
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

// Begin "SaveAs"
		saveas.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
// workaround for JFileChooser: wait until it is created
				if (wo_fc)
					wait_thread_fc();
				int returnVal = fc.showSaveDialog(VisualAp.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) 
					   try
					   {
							fname = fc.getSelectedFile().getPath();
							activePanel.writeXML(new File(fname));
							activePanel.setTitle(fname);
					   }
					   catch (IOException ex)
					   {    
							ErrorPrinter.showDialog(VisualAp.this, ex);
					   }
		   }});
// End "SaveAs"

// Begin "Print"
		print.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
							PrinterJob printJob = PrinterJob.getPrinterJob();
							printJob.setPrintable(activePanel);
							if (printJob.printDialog()) {
								try {
									printJob.print();
								} catch (Exception ex) {
									ErrorPrinter.dump(ex, getUniqueID());
								}
							}
			}});
		print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
// End "Print"

// Begin "Quit"
		quit.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				quit();
		   }});
// End "Quit"

		file.add(newWin);
		file.add(open);
		file.addSeparator();
		file.add(save);
		file.add(saveas);
		file.addSeparator();
		file.add(print);
		file.addSeparator();
		file.add(quit);
		return file;
    }

    protected JMenu buildEditMenu() {
		JMenu editMenu = new JMenu("Edit");
		insert.setEnabled(false);

		cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));

		editMenu.add(edit);
		editMenu.addSeparator();
		editMenu.add(cut);
		editMenu.add(unbind);
		editMenu.add(copy);
		editMenu.add(paste);
		editMenu.addSeparator();
		editMenu.add(insert);
		return editMenu;
	}

// showErrorDialog() pop up an error box that prompts users if help is needed
	protected boolean showErrorDialog(String msg) {
		Object[] options = { "OK", "Help" };
		return JOptionPane.showOptionDialog(VisualAp.this, msg, "Error",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE,null, options, options[0]) == 1;
	}

	protected JMenu buildSystemMenu() {
		JMenu system = new JMenu("System");
		check.setEnabled(false);
		run.setEnabled(false);
		editprops.setEnabled(false);

		check.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				Check chksys = new Check(activePanel.nodeL, activePanel.EdgeL); 
				try	{
					chksys.checkSystem();
					JOptionPane.showMessageDialog(VisualAp.this, "System check passed", WindowTitle, JOptionPane.INFORMATION_MESSAGE, icon);
				} catch (CheckException ex) {
//					JOptionPane.showMessageDialog(VisualAp.this, ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
					if (showErrorDialog(ex.getMessage()))
						hWindow.setPage(VisualAp.class.getResource("helpfile5a.html"));
					if (chksys.getErrorList() != null)	{
						activePanel.clear_selection();
						activePanel.selection.addAll(chksys.getErrorList());
						activePanel.repaint();
					}
				}
		   }});
		run.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater((Runnable) () -> {
                    Check chksys = new Check(activePanel.nodeL, activePanel.EdgeL);
                    try	{
                        StringBuffer error = new StringBuffer();
                        Engine engine = new Engine(VisualAp.this);
                        engine.runDialog(chksys.checkSystem(), error);
                        engine.join();
                        if (engine.cancel)
                            JOptionPane.showMessageDialog(VisualAp.this, "Run cancelled", "VisualAp", JOptionPane.INFORMATION_MESSAGE, icon);
                        else if (error.length() == 0)
                                JOptionPane.showMessageDialog(VisualAp.this, "Run successful", "VisualAp", JOptionPane.INFORMATION_MESSAGE, icon);
                            else JOptionPane.showMessageDialog(VisualAp.this, error,"Error",JOptionPane.ERROR_MESSAGE);
                    } catch (CheckException ex) {
//							JOptionPane.showMessageDialog(VisualAp.this, ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                        if (showErrorDialog(ex.getMessage()))
                            hWindow.setPage(VisualAp.class.getResource("helpfile5a.html"));
                        if (chksys.getErrorList() != null)	{
                            activePanel.clear_selection();
                            activePanel.selection.addAll(chksys.getErrorList());
                            activePanel.repaint();
                        }
                    } catch (InterruptedException  ex) {
                        JOptionPane.showMessageDialog(VisualAp.this, ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                    }
                });
		   }});
		editprops.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				DialogPref.showDialog();

		   }});

		system.add(check);
		system.add(run);
		system.addSeparator();
		system.add(editprops);
		return system;
    }

	protected JMenu buildToolsMenu() {
		JMenu tools = new JMenu("Tools");
		prefer.setEnabled(false);
		prefer.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				DialogPref.showDialog();

		   }});

		tools.add(prefer);
		return tools;
    }


	protected JMenu buildHelpMenu() {
		JMenu help = new JMenu("Help");
		JMenuItem openHelp = new JMenuItem("Help Topics...");
		JMenuItem checkVersion = new JMenuItem("Check Version...");
		JMenuItem about = new JMenuItem("About "+WindowTitle+"...");
		openHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

		openHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hWindow.setPage(VisualAp.class.getResource("HelpFile.html"));
			}});

		checkVersion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WebFetch fetch = new WebFetch();
				try {
					String str = fetch.fetchURL(hostingWebServer+"download.php?filename=visualap.html","<span id=version>");
					if (str != null)
						if (str.equals(Setup.version))
							JOptionPane.showMessageDialog(VisualAp.this, "No need to upgrade", "Check Version", JOptionPane.INFORMATION_MESSAGE, largeicon);
						else {
							Desktop.getDesktop().browse(java.net.URI.create(hostingWebServer+"download.php?filename=visualap.html"));
// JDK 1.5:					new BareBonesBrowserLaunch().openURL(hostingWebServer+"download.php?filename=visualap.html");
						}
					else
						JOptionPane.showMessageDialog(VisualAp.this, "Technical problems\nRetry later","Error",JOptionPane.ERROR_MESSAGE);
				}
				catch (java.net.ConnectException cex) {
//					JOptionPane.showMessageDialog(VisualAp.this, "Connection cannot be setup\nPlease read help","Error",JOptionPane.ERROR_MESSAGE);
					if (showErrorDialog("Connection cannot be setup\nPlease read help"))
						hWindow.setPage(VisualAp.class.getResource("helpfile5a.html"));
				}
				catch (IOException ex) {
					JOptionPane.showMessageDialog(VisualAp.this, "Technical problems\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
				}

			}});

		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(VisualAp.this, ABOUTMSG, "About "+WindowTitle, JOptionPane.INFORMATION_MESSAGE, largeicon);
			}});

		help.add(openHelp);
		help.add(addHelpItems("Help on beans...", hWindow));
		help.addSeparator();
		help.add(checkVersion);
		help.addSeparator();
		help.add(about);

		return help;
    }


	public void showFloatingMenu(MouseEvent e) {
		if (e.isPopupTrigger()) {
			savedPoint = e.getPoint();
			//Create the popup menu.
			JPopupMenu popup = new JPopupMenu();
			if (activePanel.selection.size() == 0) popup.add(newItem("New"));
			else {
				if (activePanel.selection.size() == 1) {
					popup.add(editItem("Properties"));
					popup.addSeparator();
				}
				popup.add(cutItem("Cut"));
				popup.add(unbindItem("Unbind"));
				popup.add(copyItem("Copy"));
			}
			if (copyL.size() != 0) popup.add(pasteItem("Paste"));
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public JMenuItem editItem(String text) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
//				if ((selection.size() == 1)&&(selection.get(0).edit())) repaint();
				if (activePanel.selection.size() == 1) activePanel.properties(activePanel.selection.get(0));
		   }});
		return menuItem;
	}

	public JMenu addHelpItems(String text, HelpWindow hWin) {
		hWindow = hWin;
		JMenu newMenu = new JMenu(text);
		ActionListener newAction = new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				JMenuItem source = (JMenuItem)(e.getSource());
				String action = source.getText();
				for (int i = 0; i < beans.size(); i++) {
					if (beans.get(i).name.equals(action)) {
						URL help = beans.get(i).helpfile;
						hWindow.setPage(help);
						return;
					}
				}
		   }}; 
		for (int i = 0; i < beans.size(); i++) {
//			Class clazz = beanList.get(i);
			if (beans.get(i).helpfile != null) {
				String name = beans.get(i).name;
				newMenu.add(new JMenuItem(name)).addActionListener(newAction);
			}	
		}	
		return newMenu;
	}
	
	public JMenu newItem(String text) {
		JMenu newMenu = new JMenu(text);
		ActionListener newAction = new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				JMenuItem source = (JMenuItem)(e.getSource());
				String action = source.getText();
				if (action.equals("Add Text")) {	
// corrected fault: NodeText instead of NodeBean
					Node n = new NodeText(new Point(savedPoint), "text"); savedPoint.translate(4, 4);
					activePanel.nodeL.add(n, "text$0");
					activePanel.clear_selection();
					activePanel.selection.add(n);
					repaint();
					return;
				}
				for (int i = 0; i < beans.size(); i++) {
					if (beans.get(i).name.equals(action)) {
						try {
							Object myBean = beans.get(i).clazz.newInstance();
							NodeBean n = new NodeBean(new Point(savedPoint), myBean);
							n.setContext(activePanel.globalVars);
							activePanel.nodeL.add(n, activePanel.shortName(action));
							activePanel.clear_selection();
							activePanel.selection.add(n);
							repaint();
						} catch (Exception ex) {
							ErrorPrinter.printInfo("instantiation of a new bean failed"+ ex);
						}
						return;
					}
				}
		   }}; 
		JMenuItem mi = new JMenuItem("Add Text");
		mi.setToolTipText("Insert text");
		newMenu.add(mi).addActionListener(newAction);		
		for (BeanDelegate bean : beans) {
			mi = new JMenuItem(bean.name);
			mi.setToolTipText(bean.toolTipText);
			newMenu.add(mi).addActionListener(newAction);		
		}
		return newMenu;
	}

	public JMenuItem cutItem(String text) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
			   if (activePanel.selection.size() > 0) {
// il problema java.util.ConcurrentModificationException � stato risolto introducendo la lista garbage
					HashSet<Edge> garbage = new HashSet<Edge>();
					for (Node t : activePanel.selection) {
						for (Edge c : activePanel.EdgeL)
							if ((c.from.getParent() == t)||(t == c.to.getParent()))
								garbage.add(c);
						activePanel.nodeL.remove(t);
					}
					for (Edge c : garbage)
						activePanel.EdgeL.remove(c);
					activePanel.clear_selection();
					repaint();
			   }
		   }});
		return menuItem;
	}

	public JMenuItem unbindItem(String text) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
			   if (activePanel.selection.size() > 0) {
// il problema java.util.ConcurrentModificationException � stato risolto introducendo la lista garbage
					HashSet<Edge> garbage = new HashSet<Edge>();
					for (Node t : activePanel.selection) {
						for (Edge c : activePanel.EdgeL)
							if ((c.from.getParent() == t)||(t == c.to.getParent()))
								garbage.add(c);
					}
					for (Edge c : garbage)
						activePanel.EdgeL.remove(c);
					activePanel.clear_selection();
					repaint();
			   }
		   }});
		return menuItem;
	}

	public JMenuItem copyItem(String text) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
			   if (activePanel.selection.size() > 0) {
				   copyL.clear();
					for (Node t : activePanel.selection)
						try {
							Node clone = t.clone();	clone.setLabel(t.getLabel());
							copyL.add(clone);
							if (clone instanceof NodeBean)
								((NodeBean)clone).setContext(activePanel.globalVars);
						} catch(CloneNotSupportedException ex) {
							ErrorPrinter.printInfo("CloneNotSupportedException");
						}
			   }
		   }});
		return menuItem;
	}

	public JMenuItem pasteItem(String text) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
			   if (copyL.size() > 0) {
					activePanel.clear_selection();
					activePanel.selection.addAll(copyL);
					copyL.clear();
					for (Node t : activePanel.selection) {
						try {
							Node clone = t.clone();	clone.setLabel(t.getLabel());
							activePanel.nodeL.add(t, t.getLabel());
							copyL.add(clone);
							if (clone instanceof NodeBean)
								((NodeBean)clone).setContext(activePanel.globalVars);
						} catch(CloneNotSupportedException ex) {
							ErrorPrinter.printInfo("CloneNotSupportedException");
						}
					}
//					copyL.clear(); copyL.addAll(activePanel.selection);
					repaint();
			   }
		   }});
		return menuItem;
	}

// checkAndSave returns true only in case of CANCEL_OPTION
	protected boolean checkAndSave() {
		if ((activePanel != null)&&(activePanel.nodeL.isChanged()))	{
			int returnVal = JOptionPane.showConfirmDialog(null, "Save changes to "+fname+" ?", "Warning", 
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (returnVal == JOptionPane.CANCEL_OPTION) return true;
			if (returnVal == JOptionPane.OK_OPTION) {
				try {
					activePanel.writeXML(new File(fname));
					activePanel.nodeL.setChanged(false);
				} catch (Exception ex) {
					ErrorPrinter.dump(ex, getUniqueID());
				}
			}
		}
		return false;
	}


	protected void quit() {
		if (checkAndSave()) return;
		try {
			prefs.flush();
		} catch (BackingStoreException ex) {	ErrorPrinter.dump(ex, getUniqueID());}
	//	System.err.println("Shutting down"); wait_thread_fc();
		System.exit(0);
	}

	public static String [] getUniqueID() {
		return new String [] {"VisualAp: "+ObjectStreamClass.lookup(VisualAp.class).getSerialVersionUID()};
	}

// special function to load all vas files and save them, useful to perform bulk file format updateing
	public static void refreshXML() {
		File currentdir = new File(".");
		File [] af = currentdir.listFiles();
		int nf = 0;
		if (af == null)
		{	System.out.println("No file to process in "+ currentdir);
			return;
		}
        for (File value : af) {
            String fname = value.getPath();
            if (!value.isDirectory())
                if (checkExt(fname, "vas")) {
                    nf += 1;
                    System.out.println("Processing file " + fname);
                    try {
                        HashSet<String> updatel = new HashSet<>();
                        File file = new File(fname);
                        activePanel.readXML(file, updatel);
                        activePanel.writeXML(file);
                    } catch (VersionException ex) {
                        System.err.println(ex);
                    } catch (IOException ex) {
                        System.err.println(ex);
                    }


                }
        }
		System.out.print("Processed "+nf+" files");
	}

	static boolean checkExt(String s, String ext) {
		int i = s.lastIndexOf('.');
		if (i == -1)  return (false);
		return (s.substring(i+1).toLowerCase().matches(ext.toLowerCase()));
	}

	public static String getAppName() {
		return VisualAp.class.getName().split("\\.")[1];
	}
	
	private static void remove_prefs() {
		prefs.remove("version");
		prefs.remove("dataPath");
		prefs.remove("beansPath"); // legacy (was defined in version 1.2.0 and removed in 1.2.1)
	}


	public static void main(String[] args) throws ParserException {
		if ((args.length == 1)&&(args[0].equals("-Xremoveprefs"))) {
// nonstandard option -Xremoveprefs is used to remove all prefs
			remove_prefs();
			System.exit(0);
		} else if ((args.length == 1)&&(args[0].equals("-Xprefs"))) {
// nonstandard option -Xprefs is used to export all prefs
			try {
				prefs.exportSubtree(System.out);
			} catch (Exception e) { }
			System.exit(0);
		}
		
		String datapath = Setup.getDataPath(); // note: this function will set the datapath if not already defined, as part of the installation process for Windows users
// nonstandard option -XupdateVAS is used to update all VAS files
		if ((args.length == 1)&&(args[0].equals("-XupdateVAS"))) {
			activePanel = new GPanel(new LoadBeans().load(datapath+File.separatorChar+"beans"), null);
			refreshXML();
			System.exit(0);
		}
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) { }

		Parser cli = new Parser();
		parser.Option fast = cli.addOption("-fast", false, null, "fast startup");
		parser.Option run = cli.addOption("-run", false, null, "automatic run");
		parser.Option report = cli.addOption("-report", false, null, "print a report about available beans");
		parser.Option uniqueID = cli.addOption("-uniqueID", false, null, "print uniqueID");
		parser.Option help = cli.addOption("-help", false, null, "this help");
		String [] result = cli.parse(args);
		if (cli.hasOption(uniqueID)) {
			String [] hc = getUniqueID();
            for (String s : hc) System.out.println(s);
			System.exit(0);
		}
		if (cli.hasOption(report)) {
			System.out.println("Report generated by " + getAppName() + " " + Setup.version);
			System.out.println("Beans directory: " + datapath+File.separatorChar+"beans");
			System.out.println("================================");
			ArrayList<BeanDelegate> beans = new LoadBeans().load(datapath+File.separatorChar+"beans");
			if (beans.size() == 0)
				System.out.println("No valid components found in directory "+datapath+File.separatorChar+"beans");
			for (int i=0; i < beans.size(); i++) {
				BeanDelegate bean = beans.get(i);
				System.out.println("Component "+bean.name);
				System.out.println("Description: "+bean.toolTipText);
				System.out.println("Version: "+bean.version);
				System.out.println("SerialUID: "+bean.serialUID);
				System.out.println("Specification Version: 1.0");
				if (bean.input.length == 0)
					if (bean.output.length == 0)
						System.out.println("Type of javabean: BlackBox");
					else {
						System.out.print("Type of javabean: Source");
						try {
							bean.clazz.getMethod("iterate",new Class[0]);	
							System.out.print(" iterative");
						}
						catch (NoSuchMethodException ex) {} // don't care
						System.out.println();
					}
				else if (bean.output.length == 0)
					System.out.println("Type of javabean: Sink");
				else System.out.println("Type of javabean: Processor");
				System.out.println();
				System.out.println("Number of inputs:"+bean.input.length);
				for (int j=0; j < bean.input.length; j++)
					System.out.println("Input["+j+"]:"+bean.input[j].toString());
				System.out.println("Number of outputs:"+bean.output.length);
				for (int j=0; j < bean.output.length; j++)
					System.out.println("Output["+j+"]:"+bean.output[j].toString());
				System.out.println("============================");
			}
			System.exit(0);
		}		
		if (cli.hasOption(help)) {
			System.out.println("Usage: java visualap.VisualAp "+cli.getUsage("<filename>"));
			System.exit(0);
		}
		if (!new File(datapath+File.separatorChar+"beans").exists()) {
			System.err.println("Error: beans directory does not exist, please reinstall the application");
			System.exit(0);
		}

		autorun = cli.hasOption(run);
		wo_fc = cli.hasOption(fast); // use workaround for filechooser
		if (result.length == 1)
			new VisualAp(result[0]);
		else if (autorun)
			System.err.println("<filename> must be specified when -run option is used");
		else if (result.length == 0) new VisualAp(null);
	}

}