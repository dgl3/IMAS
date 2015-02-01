package cat.urv.imas.graph;

import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import java.io.Serializable;
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
public class Graph implements Serializable{
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
        /**
        System.out.println(nodes);
        for(Node n: edgesMap.keySet()){
            System.out.println("Node in position: "+n.getCell().getRow()+","+n.getCell().getCol());
            List<Edge> edges = edgesMap.get(n);
            System.out.println(edges);
        }
        System.out.println("Graph created correctly");
        **/
    }

    /**
     * Returns a List containing the minimum path from the initialPoint
     * to the target Point
     * @param initialPoint Cell
     * @param targetPoint Cell
     * @return List<Node> Optimum path
     */
    public Path bfs(Cell initialPoint, Cell targetPoint, int maxPath){
        resetGraph();
        
        List<Node> path = new ArrayList<>();
        
        Queue<Node> FIFOqueue = new LinkedList<>(); //Using a LinkedList as Queue
        Node initialNode = this.nodes.get(initialPoint);
        Node targetNode = this.nodes.get(targetPoint);
        
        List<Node> visited = new ArrayList<>();

        //Get neighbours
        for(Edge edge:edgesMap.get(initialNode)){
            if(edge.getNode2().equals(targetNode)){
                path.add(edge.getNode2());
                Path optimumPath = new Path(path);
                return optimumPath;
            }
            Node childNode = edge.getNode2();
            childNode.setPreviousNode(initialNode);
            childNode.setDistance(1);
            FIFOqueue.add(childNode);  
        }
        visited.add(initialNode);
        
        Node found = null;
        
        //Explore the graph until the final state is found or the distance 
        //to nodes is greater than 18
       if(FIFOqueue.isEmpty()){
            return null;
       }
       Node neighbour = FIFOqueue.poll();

       while(found == null && neighbour.getDistance() < maxPath){
           visited.add(neighbour);
           List<Node> unvisitedChilds = getUnvisitedChildNodes(neighbour,visited);
           for(Node child: unvisitedChilds){
                child.setPreviousNode(neighbour);
                child.setDistance(neighbour.getDistance()+1);
                if(child.equals(targetNode)){
                    path.add(child);
                    found = child;
                }
                FIFOqueue.add(child);
           }
            if(FIFOqueue.isEmpty()){
              return null;
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
               }   
               currentNode = currentNode.getPreviousNode();
            }
            Collections.reverse(path);
            /**
             * I'm deleting the inital state from the path, so actually, 
             * the fist state of the path is the next state
             */
            path.remove(0);
            
            
            Path optimumPath = new Path(path);
            return optimumPath;
        }else{
            return null;
        }

    }
    
    
    public Path bfs(Cell initialPoint, Cell targetPoint, int maxPath, Cell restriction){
        resetGraph();
        
        List<Node> path = new ArrayList<>();
        
        Queue<Node> FIFOqueue = new LinkedList<>(); //Using a LinkedList as Queue
        Node initialNode = this.nodes.get(initialPoint);
        Node targetNode = this.nodes.get(targetPoint);
        Node restrictedNode = this.nodes.get(restriction);
        
        List<Node> visited = new ArrayList<>();

        //Get neighbours
        for(Edge edge:edgesMap.get(initialNode)){
            if(!edge.getNode2().equals(restrictedNode)){
                if(edge.getNode2().equals(targetNode)){
                    path.add(edge.getNode2());
                    Path optimumPath = new Path(path);
                    return optimumPath;
                }
                Node childNode = edge.getNode2();
                childNode.setPreviousNode(initialNode);
                childNode.setDistance(1);
                FIFOqueue.add(childNode);                
                }
        }
        visited.add(initialNode);
        
        Node found = null;
        
        //Explore the graph until the final state is found or the distance 
        //to nodes is greater than 18
       if(FIFOqueue.isEmpty()){
            return null;
       }
       Node neighbour = FIFOqueue.poll();

       while(found == null && neighbour.getDistance() < maxPath){
           visited.add(neighbour);
           List<Node> unvisitedChilds = getUnvisitedChildNodes(neighbour,visited);
           for(Node child: unvisitedChilds){
               if(!child.equals(restrictedNode)){
                    child.setPreviousNode(neighbour);
                    child.setDistance(neighbour.getDistance()+1);
                    if(child.equals(targetNode)){
                        path.add(child);
                        found = child;
                    }
                    FIFOqueue.add(child);                   
               }
           }
            if(FIFOqueue.isEmpty()){
              return null;
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
               }   
               currentNode = currentNode.getPreviousNode();
            }
            Collections.reverse(path);
            /**
             * I'm deleting the inital state from the path, so actually, 
             * the fist state of the path is the next state
             */
            path.remove(0);
            
            
            Path optimumPath = new Path(path);
            return optimumPath;
        }else{
            return null;
        }        
    }
    
    

    /**
     * It is the method to be called by the firemen and the ambulances to get
     * the optimum path between them and the building in fire or the hospital.
     *
     * It returns null if the path is longer than 18, as this is the max sensible number for firemen.
     *
     * @param init Cell of the fireman or ambulance
     * @param target Cell of the building in fire or hospital
     * @return Agent's optimum path from the init position to the desired target.
     */
    public Path computeOptimumPath(Cell init, Cell target, int maxDist){
        List<Cell> adjacentCells = getAdjacentCells(target);
        Path optimumPath = null;
        for(Cell cell: adjacentCells){
            if(!init.equals(cell)){
                Path path = bfs(init, cell, maxDist);
                if (path != null){//No path with distance < 18
                    if(optimumPath == null){
                        optimumPath = path;
                    }else{
                        if(path.getDistance() < optimumPath.getDistance()){
                            optimumPath = path;
                        }
                    }        
                }
            }else{
                optimumPath = new Path(null,0);
            }
        }
        return optimumPath;
    }

    /**
     * It is the method to be called by the firemen and the ambulances to get
     * the optimum path between them and the building in fire or the hospital.
     *
     * @param init Cell of the fireman or ambulance
     * @param target Cell of the building in fire or hospital
     * @return Agent's optimum path from the init position to the desired target.
     */
    public Path computeOptimumPathUnconstrained(Cell init, Cell target){
        List<Cell> adjacentCells = getAdjacentCells(target);
        Path optimumPath = null;
        for(Cell cell: adjacentCells){
            if(!init.equals(cell)){
                Path path = bfs(init, cell, Integer.MAX_VALUE);
                if(optimumPath == null){
                    optimumPath = path;
                }else{
                    if(path.getDistance() < optimumPath.getDistance()){
                        optimumPath = path;
                    }
                }                            
            }else{
                optimumPath = new Path(null,0);                
            }
        }
        return optimumPath;
    }
  
    
   /**
     * Same as the method computeOptimumPath() but with restriction
     * @param init Cell of the fireman or ambulance
     * @param target Cell of the building in fire or hospital
     * @param restriction Cell unavailable
     * @return 
     */
    public Path computeOptimumPathWithRestrictions(Cell init, Cell target, Cell restriction){
        List<Cell> adjacentCells = getAdjacentCells(target);
        Path optimumPath = null;
        for(Cell cell: adjacentCells){
            if(!init.equals(cell)){
                Path path = bfs(init, cell, 18, restriction);
                if (path != null){//No path with distance < 18
                    if(optimumPath == null){
                        optimumPath = path;
                    }else{
                        if(path.getDistance() < optimumPath.getDistance()){
                            optimumPath = path;
                        }
                    }        
                }
            }else{
                optimumPath = new Path(null,0);
            }
        }
        return optimumPath;
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
    
    /**
     * 
     * @param cellTarget
     * @return 
     */
    public List<Cell> getAdjacentCells(Cell cellTarget){
        List<Cell> adjacentCells = new ArrayList<Cell>();
        for(int i=cellTarget.getRow()-1;i<=cellTarget.getRow()+1;i++){
            for(int j=cellTarget.getCol()-1;j<=cellTarget.getCol()+1;j++){
                Cell currentCell = new StreetCell(i,j);
                Node current = nodes.get(currentCell);
                if(current != null){
                    adjacentCells.add(currentCell);
                }
            }
        }            
        return adjacentCells;
    }    

}
