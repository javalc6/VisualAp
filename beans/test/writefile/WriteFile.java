// ONLY FOR DEMO PURPOSE

package test.writefile;
import common.SampledAudio;
import java.io.*;
import javax.sound.sampled.*;	
import java.awt.image.BufferedImage;
//import com.sun.image.codec.jpeg.*; obsolete
import javax.imageio.*;
import javax.imageio.stream.*;

/**
 * 
 * WriteFile is a component used to write the content of files.
 * Note: visualap components shall implement Serializable and Cloneable
 * 
 * @author      javalc6
 * @version     1.1
 */

public class WriteFile implements Serializable, Cloneable {
	public static final long serialVersionUID = -8212411831174886841L;
	public static final String version = "1.1";

    private String aFile = "";
	private PrintWriter out;
	private ByteArrayInputStream bais;
	private SequenceInputStream seq;
	private AudioFormat format;
	private int audioLength;

	private int filetype;

/**
* Returns a string that provide short information about the component
* 
* @return      the short information about the component
*/

	public static String getToolTipText() {
		return "write data to a file";
	}

/**
* start the iteration process
* 
* @param blocksize Number of samples, used only for sampled sources
*/
	public void start() throws IOException {
		filetype = FileEditorW.fileType(aFile);
		switch (filetype) {
			case 1:  // text file
				out = new PrintWriter(new FileWriter(FileEditorW.getPath()+File.separatorChar+aFile)); 
				break;
			case 10:  // image file
				break;
			case 20:  // audio file
				bais = null;
				seq = null;
				audioLength = 0;
				break;
			default: if (aFile != "")
						throw new IOException("Invalid file type");
		}
	}
/**
* Performs needed action when iterations are stopped
* 
*/
	public void stop() throws IOException {
		switch (filetype) {
			case 1:  // text file
				out.close();
				break;
			case 10:  // image file
				break;
			case 20:  // audio file
				AudioInputStream ais = null;
				if (seq == null) {
					if (bais != null)
						ais = new AudioInputStream(bais, format, 
						audioLength / format.getFrameSize());
				} else	ais = new AudioInputStream(seq, format, 
						audioLength / format.getFrameSize());
				
				if ((ais != null)&&(AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(FileEditorW.getPath()+File.separatorChar+aFile)) == -1)) {
					throw new IOException("Problems writing to file");
				}
				break;
		}
	}


/**
* FileName getter
* 
* @return      FileName
*/
    public String getFile(){
        return aFile;
    }
 
/**
* FileName setter
* 
* @param newFile FileName
*/
    public void setFile(String newFile){
        aFile = newFile;
    }
/**
* Performs cloning of the current object
* 
* @return      the cloned object
*/
	public Object clone() {
		WriteFile cloning = new WriteFile();
		cloning.setFile(aFile);
		return cloning;
	}

/**
* Write file content
* 
* @param obj Object to write on file
*/
    public void write(Object obj) throws IOException {
		if (obj == null) return;
		if (aFile == "") // no filename provided
			if (obj instanceof BufferedImage) {
				aFile = "noname.jpg";
				filetype = 10;
			} else if (obj instanceof SampledAudio) {
				aFile = "noname.wav";
				filetype = 20;
			} else throw new IOException("No filename provided");

		switch (filetype) {
			case 1:  // text file
				if (obj instanceof String)
					out.println((String)obj);
				else if (obj instanceof String[])
						for (int k=0; k < ((String[])obj).length; k++)
							out.println(((String[])obj)[k]);
					else throw new IOException("String type expected");
				break;
			case 10:  // image file
				if (!(obj instanceof BufferedImage))
					throw new IOException("BufferedImage type expected");
/* old code replaced by javax.imageio
				BufferedOutputStream bos =
				  new BufferedOutputStream(new FileOutputStream(FileEditorW.getPath()+File.separatorChar+aFile));
				JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(bos);
				JPEGEncodeParam jep = encoder.getDefaultJPEGEncodeParam((BufferedImage)obj);
				jep.setQuality(1.0f, false);
				encoder.setJPEGEncodeParam(jep);
				encoder.encode((BufferedImage)obj);
				bos.close();
*/				String suffix = aFile.substring(aFile.lastIndexOf('.') + 1);
				java.util.Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix(suffix);

				File f = new File(FileEditorW.getPath()+File.separatorChar+aFile);
				FileImageOutputStream fios = new FileImageOutputStream(f);
				ImageWriter iw = iter.next();
				if (iw == null)
					throw new IIOException("No image writers available for " + suffix);
				iw.setOutput(fios);
				iw.write((BufferedImage)obj);
				fios.close();
				break;
			case 20:  // audio file
				if (!(obj instanceof SampledAudio))
					throw new  IOException("SampledAudio type expected");
				SampledAudio sa = (SampledAudio)obj;
				audioLength += sa.length;
				// clone the buffer in order to avoid problems in creating the file
				byte[] clone = new byte[sa.length];
				System.arraycopy(sa.buffer, 0, clone, 0, sa.length);
				if (seq == null)
					if (bais == null) {
						bais = new ByteArrayInputStream(clone);
						format = sa.format;
					}
					else {
						ByteArrayInputStream bais1 = new ByteArrayInputStream(clone);
						seq = new SequenceInputStream(bais, bais1);
					}
				else {
					ByteArrayInputStream bais1 = new ByteArrayInputStream(clone);
					seq = new SequenceInputStream(seq, bais1);
				}
				break;
			default: throw new IOException("Invalid file type");
		}
	}

}
