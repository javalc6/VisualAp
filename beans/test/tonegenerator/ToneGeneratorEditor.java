/* class ToneGeneratorEditor

This class provides a custom editor to select a specific name

version 0.5, 01-03-2011, added saw

javalc6
*/

package test.tonegenerator;
public class ToneGeneratorEditor extends java.beans.PropertyEditorSupport {

    public String[] getTags() {
		String result[] = {
			"Saw",
			"Sine",
			"Square",
			"Triangle"};
		return result;
    }

    public String getJavaInitializationString() {
		return (String)getValue();
    }

}

