/*
Version 1.0, 30-12-2007, First release
Version 1.2.1, 13-03-2011, changed type of errorList: ArrayList<Node>

IMPORTANT NOTICE, please read:

This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE,
please read the enclosed file license.txt or http://www.gnu.org/licenses/licenses.html

Note that this software is freeware and it is not designed, licensed or intended
for use in mission critical, life support and military purposes.

The use of this software is at the risk of the user.
*/

/* class Check

This class is used to check the system

javalc6
*/
package visualap;
import java.lang.reflect.*;
import java.util.ArrayList;
import graph.*;

public class Check {
	private ArrayList<NodeBean> nodeL;
	private Edges EdgeL;
	private int nextMark, lostMark;
	private ArrayList<Node> errorList;

	public Check(GList<Node> nodeL, Edges EdgeL) {
		this.nodeL = new ArrayList<NodeBean>();
		for (Node aNode : nodeL)
			if (aNode instanceof NodeBean) 
				this.nodeL.add((NodeBean)aNode);		

		this.EdgeL = EdgeL;
		errorList = new ArrayList<Node>();
	}

/* graph analysis
the method checkSystem() perform analysis of the graph, checking if it conforms to VisualAp rules and that is acyclic
*/
	public Vertex [] checkSystem() throws CheckException {
		errorList.clear();
		return sortSystem(buildMBranch());
	}

	public ArrayList<Node> getErrorList() {
		return errorList;
	}

		private void updateBranch(Pin from, Pin to) {
			int marka, markb;
			marka = from.getMark();
			markb = to.getMark();
			if (marka == 0)
				if (markb == 0) {
					from.setMark(nextMark);
					to.setMark(nextMark);
					nextMark++;
				} else from.setMark(markb);
			else if (markb == 0) 
					to.setMark(marka);
				else if (marka != markb) {
						for (NodeBean t : nodeL) {
							for (int i = 0; i < t.inPins.length; i++) {
								if (t.inPins[i].getMark() == markb)
									t.inPins[i].setMark(marka);
							}			
							for (int i = 0; i < t.outPins.length; i++) {
								if (t.outPins[i].getMark() == markb)
									t.outPins[i].setMark(marka);
							}								
						}						
						lostMark++;
					}
		}

	private Vertex [] buildMBranch() throws CheckException {
		if (nodeL == null) return null;
// reset mark on all pins
		for (NodeBean t : nodeL) {
			for (int i = 0; i < t.inPins.length; i++) {
				t.inPins[i].setMark(0);
			}			
			for (int i = 0; i < t.outPins.length; i++) {
				t.outPins[i].setMark(0);
			}			
		}
		nextMark = 1; lostMark = 0;
// update mark using list of Edges
		for (Edge t : EdgeL) {
			updateBranch(t.from,t.to);
		}
//		if (nextMark == 1) throw new CheckException("There are no connections");
		int nout = 0;
		Backward [] pOut = new Backward[nextMark];
		Vertex [] vertexL = new Vertex [nodeL.size()];
		for (int i = 0; i < vertexL.length; i++) {
			NodeBean t = nodeL.get(i);
			if (t.getObject() != null)
				vertexL[i] = new Vertex(t);
			else vertexL[i] = null;
			for (int j = 0; j < t.outPins.length; j++) {
				int mark = t.outPins[j].getMark();
				if (mark != 0) {
					if (pOut[mark] == null) {
						pOut[mark] = new Backward();
						pOut[mark].index = t.outPins[j].getIndex();
						pOut[mark].obj = vertexL[nodeL.indexOf(t.outPins[j].getParent())];
						nout++;
					} else {
//						System.out.println("GPanel.checkSystem: "+t.getLabel()+" <-> "+ pOut[t.outPins[i].getMark()].getParent().getLabel());
						errorList.add(t);
						throw new CheckException("Detected collision between two or more output pins\nPlease check "+t.getLabel());
					}
				}
			}			
		}
        for (Vertex element : vertexL) {
            if (element != null)
                for (int j = 0; j < element.aNode.inPins.length; j++) {
                    int mark = element.aNode.inPins[j].getMark();
                    if (mark != 0) {
                        element.backward[j] = pOut[mark];
                    } else {
                        errorList.add(element.aNode);
                        throw new CheckException("Detected a floating input pin\nPlease check " + element.aNode.getLabel());
                    }
                }
        }
// compress list of vertex
		int counter = 0;
        for (Vertex item : vertexL) {
            if (item != null) counter++;
        }
		if (counter == 0) return null;
		Vertex [] vertex = new Vertex [counter];
		int j = 0;
        for (Vertex value : vertexL) {
            if (value != null) {
                vertex[j] = value;
                j++;
            }
        }
		if (nout != nextMark - lostMark - 1)
			throw new CheckException("Detected input pins not connected to any output pin");
		return vertex;
	}

	private boolean copre(Vertex a, Vertex b) {
		for (int i = 0; i < a.backward.length; i++) {
			if (a.backward[i].obj.equals(b))
				return true;
		}
		return false;
	}

	private Vertex [] sortSystem(Vertex [] vertexL) throws CheckException {
		if (vertexL == null) return null;
// first check for self-loop
        for (Vertex vertex : vertexL) {
            if (copre(vertex, vertex)) {
                errorList.add(vertex.aNode);
                throw new CheckException("Detected a loop\nPlease check " + vertex.aNode.getLabel());
            }
        }
		int first = 0;
		int last = vertexL.length - 1;
// check for any cycles
		while (last > 0) {
			int i = first+1;
			while ((i <= last)&& !copre(vertexL[i],vertexL[first])) i++;
			if (i <= last) {
				first++;
//				swap(i,first);
				Vertex t = vertexL[i]; vertexL[i] = vertexL[first]; vertexL[first] = t;
			} else {
//				swap(first,last);
				Vertex t = vertexL[first]; vertexL[first] = vertexL[last]; vertexL[last] = t;
				if (first > 0) {
					first--;
					for (i = 0; i <= first; i++)
						if (copre(vertexL[i],vertexL[last])) {
							errorList.add(vertexL[i].aNode);
							throw new CheckException("Detected a cycle\nPlease check "+vertexL[i].aNode.getLabel());
						}
				}
				last--;
			}
		}
		return vertexL;
	}
}
