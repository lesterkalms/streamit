/*
 * Created on Jun 24, 2003
 */
package streamit.eclipse.grapheditor;

import streamit.eclipse.grapheditor.jgraphextension.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.BorderFactory;
import java.io.*;
import com.jgraph.JGraph;
import com.jgraph.graph.ConnectionSet;
import com.jgraph.graph.DefaultEdge;
import com.jgraph.graph.DefaultPort;
import com.jgraph.graph.GraphConstants;

/**
 * GEFeedbackLoop is the graph internal representation of  a feedback loop.
 * @author jcarlos
 */
public class GEFeedbackLoop extends GEStreamNode implements Serializable{
	
	/**
	 * The splitter belonging to this feedback loop.
	 */
	private GESplitter splitter;
	
	/**
	 * The joiner belonging to this feedback loop.
	 */
	private GEJoiner joiner;
	
	/**
	 * The body of the feedback loop.
	 */
	private GEStreamNode body;
	
	/**
	 * The loop part of the feedback loop.
	 */
	private GEStreamNode loop;

	/**
	 * The sub-graph structure that is contained within this GEFeedbackLoop
	 * This subgraph is hidden when the GEFeedbackLoop is collapse and 
	 * visible when expanded. 
	 */
	private GraphStructure localGraphStruct;

	/**
	 * Boolean that specifies if the elements contained by the GEFeedbackLoop are 
	 * displayed (it is expanded) or they are hidden (it is collapsed).
	 */
	private boolean isExpanded;

	/**
	 * The level of how deep the elements of this splitjoin are with respect to 
	 * other container nodes that they belong to.
	 * The toplevel pipeline has level 0. The elements of another pipeline within
	 * this toplevel pipeline would have its level equal to 1 (same applies to other 
	 * container nodes such as splitjoins and feedback loops).
	 */
	private int level;


	/**
	 * GEFeedbackLoop constructor.
	 * @param name The name of the GEFeedbackLoop.
	 * @param split The GESplitter that corresponds to this GEFeedbackLoop.
	 * @param join The GEJoiner that corresponds to this GEFeedbackLoop.
	 * @param body The GEStreamNode that represents the body of theGEFeedbackLoop.
	 * @param loop The GEStreamNode that represents the body of the GEFeedbackLoop.
	 */
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, GEStreamNode body, GEStreamNode loop)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.body = body;
		this.loop = loop;
		this.localGraphStruct = new GraphStructure();
		this.isExpanded = false;
	}
	
	/**
	 * Get the splitter part of this.
	 * @return GESplitter corresponding to this GEFeedbackLoop.
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
	 * Get the joiner part of this.
	 * @return GESJoiner corresponding to this GEFeedbackLoop.
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}	
	
	/**
	 * Get the body of this.
	 * @return GEStreamNode that is the body of GEFeedbackLoop.
	 */
	public GEStreamNode getBody()
	{
		return this.body;
	}
	
	/**
	 * Get the loop of this.
	 * @return GEStreamNode that is the loop of GEFeedbackLoop.
	 */
	public GEStreamNode getLoop()
	{
		return this.loop;
	}


	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		System.out.println("Constructing the feedback loop " +this.getName());
		
		/**Set the level of this and add this to list of containers at this level **/
		this.level = lvel;
		graphStruct.addToLevelContainer(level, this);
		lvel++; // Contained elements of this will be at a higher level
		
		graphStruct.getJGraph().addMouseListener(new JGraphMouseAdapter(graphStruct.getJGraph()));	
		this.localGraphStruct = graphStruct;	
					
		joiner.construct(graphStruct, lvel);
		GEStreamNode lastBody = body.construct(graphStruct, level);
				
		System.out.println("Connecting " + joiner.getName()+  " to "+ body.getName());		
		graphStruct.connectDraw(joiner, lastBody );
	
		System.out.println("Connecting " + body.getName()+  " to "+ splitter.getName());
		graphStruct.connectDraw(body, splitter);
		
		splitter.construct(graphStruct, level);
		GEStreamNode lastLoop = loop.construct(graphStruct, level); 
		
		System.out.println("Connecting " + splitter.getName()+  " to "+ loop.getName());
		graphStruct.connectDraw(splitter, loop);
		
		System.out.println("Connecting " + loop.getName()+  " to "+ joiner.getName());
		graphStruct.connectDraw(loop, joiner);
	
		graphStruct.getGraphModel().insert(graphStruct.getCells().toArray(),graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		this.initDrawAttributes(graphStruct);
				
		return this;
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GESplitJoin.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct)
	{
		this.port = new DefaultPort();
		this.add(this.port);
		
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBorder(this.attributes , BorderFactory.createLineBorder(Color.green));
		
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(this.getChildren().toArray(), false);	
	}
	
	/**
	 * Expand or collapse the GESplitJoin structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph 
	 */	
	public void collapseExpand(JGraph jgraph)
	{
		if (isExpanded)
		{
			this.collapse(jgraph);
			isExpanded = false;
		}
		else
		{
			
			this.expand(jgraph);
			isExpanded = true;
		}
	}	
	
	/**
	 * Expand the GESplitJoin. When it is expanded the elements that it contains are
	 * displayed.
	 */
	public void expand(JGraph jgraph)
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		jgraph.getGraphLayoutCache().setVisible(nodeList, true);
		
		
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		ArrayList edgesToRemove =  new ArrayList();
		
		while (eIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) eIter.next();
			Iterator sourceIter = this.getSourceEdges().iterator();	
			while (sourceIter.hasNext())
			{
				DefaultEdge s = (DefaultEdge) sourceIter.next();
				System.out.println(" s hash" +s.hashCode());
				if (s.equals(edge))
				{
					System.out.println("source edges were equal");
					cs.disconnect(edge, true);
					cs.connect(edge, this.splitter.getPort(), true);		
					this.joiner.addSourceEdge(s);
					edgesToRemove.add(s);
				}
			}
			
			Iterator targetIter = this.getTargetEdges().iterator();
			while(targetIter.hasNext())
			{
				DefaultEdge t = (DefaultEdge) targetIter.next();
				System.out.println(" t hash" +t.hashCode());
				if(t.equals(edge))
				{
					System.out.println("target edges were equal");
					cs.disconnect(edge,false);
					cs.connect(edge, this.joiner.getPort(),false);
					this.splitter.addTargetEdge(t);
					edgesToRemove.add(t);
				}
			}
			
			Object[] removeArray = edgesToRemove.toArray();
			for(int i = 0; i<removeArray.length;i++)
			{
				this.removeSourceEdge((DefaultEdge)removeArray[i]);
				this.removeTargetEdge((DefaultEdge)removeArray[i]);
			}
	
		}
		
		
		this.localGraphStruct.getGraphModel().edit(null, cs, null, null);
		
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.hideContainersAtLevel(i);
		}
		
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		manager.arrange();	
		setLocationAfterExpand();
	}
	
	//TODO
	/**
	 * Expand the GESplitJoin. When it is expanded the elements that it contains are
	 * displayed.
	 */	
	public void collapse(JGraph jgraph)
	{			
	}


	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node, then a list with no elements is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return empty list.
	 */
	public ArrayList getContainedElements()
	{
		ArrayList tempList = new ArrayList();
	  	tempList.add(this.joiner);
	  	tempList.add(this.body);
	  	tempList.add(this.loop);
	  	tempList.add(this.splitter);

	   return tempList;
	 	
	}
	

	/**
	 * Sets the location of the Container nodes that have a level less than or equal 
	 * to this.level. The bounds of the container node are set in such a way that the 
	 * elements that it contains are enclosed.
	 * Also, changes the location of the label so that it is more easily viewable.
	 */
	private void setLocationAfterExpand()
	{
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.setLocationContainersAtLevel(i);
		}
	}
	
	/**
	 * Hide the GEStreamNode in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible.
	 * @return true if it was possible to hide the node; otherwise, return false.
	 */
	public boolean hide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, false);
		return true;
	}
	
	/**
	 * Make the GEStreamNode visible in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible. 
	 * @return true if it was possible to make the node visible; otherwise, return false.
	 */	
	public boolean unhide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, true);
		return true;
	}	
}




