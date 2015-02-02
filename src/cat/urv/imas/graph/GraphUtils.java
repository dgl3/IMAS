/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cat.urv.imas.graph;

import cat.urv.imas.map.Cell;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 *
 * @author dgl3
 */
public class GraphUtils {
    
    /**
     * Returns the action area of a cell in a graph
     * @param graph
     * @param currentCell
     * @param numActionArea
     * @return Graph with setted values for the action area
     * @throws Exception 
     */
    public static Graph actionArea(Graph graph, Cell currentCell, int numActionArea) throws Exception{
        Graph graphCloned = (Graph)graph.clone();
        Node currentNode = graphCloned.getNodes().get(currentCell);
        currentNode.setNumActionArea(numActionArea);
        //Add the node to the graph again
        
        List<Node> visited = new ArrayList<>();
        Queue<Node> FIFOqueue = new LinkedList<>(); //Using a LinkedList as Queue

        try{
            //Compute ActionArea
            int i = numActionArea;
            i-=1;
            while(i > 0){
                List<Edge> currentEdges = graphCloned.getEdges().get(currentNode);
                for(Edge edge: currentEdges){
                    if(!visited.contains(edge.getNode2())){
                        Node child = graphCloned.getNodes().get(edge.getNode2().getCell());
                        if (child.getNumActionArea() == 0){
                            child.setNumActionArea(i);
                        }

                        FIFOqueue.add(child);
                    }
                }
                i-=1;
                visited.add(currentNode);
                currentNode = FIFOqueue.poll();
            }
        }catch(NullPointerException e){
            throw new NullPointerException("The cell: "+currentCell+ " is not in the graph");
        }
        return graphCloned;
    }
    
    public Graph joinGraphs(List<Graph> graphList){
        //I'm not removing the first graph because the clone method generate a new graph from the settings
        Graph firstGraph = graphList.remove(0);
        Graph joinGraph = (Graph)firstGraph.clone();
        
        for(Graph graph: graphList){
            for(Node node: graph.getNodes().values()){
                if(node.getNumActionArea() != 0){
                    if(joinGraph.getNodes().get(node.getCell()).getNumActionArea() > node.getNumActionArea()){
                        joinGraph.getNodes().get(node.getCell()).setNumActionArea(node.getNumActionArea());
                    }
                }
            }
        }
        
        return joinGraph;
    }
    
    public int distributionValue(Graph graph){
        int value = 0;
        for(Node node: graph.getNodes().values()){
            value += node.getNumActionArea();
        }
        return value;
           
    }
    
}
