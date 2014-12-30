package cat.urv.imas.graph;

import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.onthology.GameSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author dgl3
 */
public class Graph {
    private GameSettings settings;
    
    private Map<Cell,Node> nodes; // Map of nodes
    private Map<Node,List<Edge>> edgesMap; // Map of edges
    
    /**
     * Graph constructor
     * @param settings GameSettings
     */
    
    private static int MAX_STEPS = 18;
    
    public Graph(GameSettings settings){
        this.nodes = new HashMap<>();
        this.edgesMap = new HashMap<>();
        
        this.settings = settings;
        initGraph(settings.getMap());
    }
    /**
     * Get graph's nodes
     * @return Map<Cell,Node>
     */
    public Map<Cell,Node> getNodes(){
        return nodes;
    }
    
    /**
     * Get graph's edges
     * @return Map<Node,List<Edge>>
     */
    public Map<Node,List<Edge>> getEdges(){
        return edgesMap;
    }
    
    /**
     * Creates a Graph from the map of the city, where each node represents a 
     * cell, and edges indicate the connections between nodes.
     * @param map Cell[][] Map of the city received from the GameSettings class
     */
    private void initGraph(Cell[][] map){
        int rows = map.length;
        int cols = map.length;
        Cell cell;
        //Initialize nodes
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cell = map[row][col];
                if(cell.getCellType() == CellType.STREET){
                    if(!nodes.containsKey(cell)){
                        Node node = new Node(cell);
                        nodes.put(cell, node);
                    }
                    Node node = nodes.get(cell);
                    
                    List<Edge> edgesList = new ArrayList<>();
                    edgesMap.put(node, edgesList);
                    
                    //Top node                    
                    Cell topCell = map[row-1][col];
                    if(topCell.getCellType() == CellType.STREET){
                       if(!nodes.containsKey(topCell)){
                           Node topNode = new Node(topCell);
                           nodes.put(topCell, topNode);                            
                       }
                       Edge topEdge = new Edge(node, nodes.get(topCell));
                       edgesMap.get(node).add(topEdge);                           
                    }
                    
                    //Left node                    
                    Cell leftCell = map[row][col-1]; 
                    if(leftCell.getCellType() == CellType.STREET){
                         if(!nodes.containsKey(leftCell)){
                              Node leftNode = new Node(leftCell);
                              nodes.put(leftCell, leftNode);                            
                         } 
                         Edge leftEdge = new Edge(node, nodes.get(leftCell));
                         edgesMap.get(node).add(leftEdge);                           
                    }
                    
                    //Right node                    
                    Cell rightCell = map[row][col+1]; 
                    if(rightCell.getCellType() == CellType.STREET){
                        if(!nodes.containsKey(rightCell)){
                             Node rightNode = new Node(rightCell);
                             nodes.put(rightCell, rightNode);                            
                        } 
                        Edge rightEdge = new Edge(node, nodes.get(rightCell));
                        edgesMap.get(node).add(rightEdge);                             
                    }
                    
                    //Bottom node
                    Cell bottomCell = map[row+1][col]; 
                    if(bottomCell.getCellType() == CellType.STREET){
                         if(!nodes.containsKey(bottomCell)){
                              Node bottomNode = new Node(bottomCell);
                              nodes.put(bottomCell, bottomNode);                            
                         } 
                         Edge bottomEdge = new Edge(node, nodes.get(bottomCell));
                         edgesMap.get(node).add(bottomEdge);                               
                    }
                    node.addEdges(edgesMap.get(node));
                }
            }
        }
        //Prints the graph's information
        System.out.println(nodes);
        for(Node n: edgesMap.keySet()){
            System.out.println("Node in position: "+n.getCell().getRow()+","+n.getCell().getCol());
            List<Edge> edges = edgesMap.get(n);
            System.out.println(edges);
        }
        System.out.println("Graph created correctly");
    }

    //Futur improvement: stop when the size of the optimum path is 18
    /**
     * Returns a List containing the minimum path from the initialPoint
     * to the target Point
     * @param initialPoint Cell
     * @param targetPoint Cell
     * @return List<Node> Optimum path
     */
    public Path bfs(Cell initialPoint, Cell targetPoint){
        resetGraph();
        
        List<Node> path = new ArrayList<>();
        
        Queue<Node> FIFOqueue = new LinkedList<>(); //Using a LinkedList as Queue
        Node initialNode = this.nodes.get(initialPoint);
        Node targesNode = this.nodes.get(targetPoint);
        
        //Get neighbours
        for(Edge edge:edgesMap.get(initialNode)){
            if(edge.getNode2().equals(targesNode)){
                path.add(edge.getNode2());
                Path optimumPath = new Path(path);
                return optimumPath;
            }
            Node childNode = edge.getNode2();
            childNode.setPreviousNode(initialNode);
            childNode.setDistance(1);
            FIFOqueue.add(childNode);  
        }
        List<Node> visited = new ArrayList<>();
        Node found = null;
        
        //Explore the graph until the final state is found or the distance 
        //to nodes is greater than 18
       Node neighbour = FIFOqueue.poll();

        while(found == null && neighbour.getDistance() < 18){
           if(FIFOqueue.isEmpty()){
               return null;
           } 
           visited.add(neighbour);
           List<Node> unvisitedChilds = getUnvisitedChildNodes(neighbour,visited);
           for(Node child: unvisitedChilds){
                child.setPreviousNode(neighbour);
                child.setDistance(neighbour.getDistance()+1);
                if(child.equals(targesNode)){
                    path.add(child);
                    found = child;
                }
                FIFOqueue.add(child);  
           }
        neighbour = FIFOqueue.poll();

        }
        
        
        //Get the optimum path
        if(found != null){
            Node currentNode = found;
            while(true){
               if(visited.contains(currentNode.getPreviousNode())){
                   visited.remove(currentNode.getPreviousNode());
                   path.add(currentNode.getPreviousNode());
                   if(currentNode.getPreviousNode().equals(initialNode)){
                       break;
                   }
                   currentNode = currentNode.getPreviousNode();
               }               
            }
            Collections.reverse(path);
            Path optimumPath = new Path(path);
            return optimumPath;
        }else{
            return null;
        }

    }
    /**
     * Checks and return the list of unvisited neighbours corresponding current
     * node.
     * @param node current node
     * @param visited list of visited nodes
     * @return  list of the unvisited neighbours of "node"
     */
    public List<Node> getUnvisitedChildNodes(Node node, List<Node> visited){
        List<Edge> childs = edgesMap.get(node);
        List<Node> unvisitedChilds = new ArrayList<>();
        for(Edge edge: childs){
            if(!visited.contains(edge.getNode2())){
                unvisitedChilds.add(edge.getNode2());
            }
        }
        return unvisitedChilds;
    }
    
    /**
     * Resets the previousNode of each Node in the graph
     */
    public void resetGraph(){
        for(Node node: nodes.values()){
            node.setPreviousNode(null);
            node.setDistance(0);
        }
    }
    

}
