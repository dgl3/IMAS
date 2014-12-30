
package cat.urv.imas.graph;

import cat.urv.imas.map.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author dgl3
 */
public class Node {
    /**
     * Represent connections between other nodes and this one.
     */
    private List<Edge> edges; 
    
    /**
     * Position of the node in the map (i,j)
     */
    private Cell cell;
    

    private int distance = 0;
    private Node previousNode;
    /**
     * Node class constructor. Initialization.
     * @param cell Position of the node in the map
     */
    public Node(Cell cell){
        edges = new ArrayList<Edge>();
        this.cell = cell;
    }
    
    /**
     * Get the list of edges, "neighbours" of that node
     * @return List<Edge>
     */
    public List<Edge> getEdges(){
        return edges;
    }
    
    /**
     * Returns the Cell corresponding to the node
     * @return Cell 
     */
    public Cell getCell(){
        return cell;
    }
    
    /**
     * Adds a new edge("neighbour") to the node
     * @param edge 
     */
    public void addEdge(Edge edge){
        edges.add(edge);
    }
    
    /**
     * Checks if the node contains the specific edge
     * @param edge Edge
     * @return true, if the node has the edge
     *          false, if the node hasn't the edge
     */
    public boolean hasEdge(Edge edge){
        return edges.contains(edge);
    }

    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.cell);
        return hash;
    }
    
    @Override
    public boolean equals(Object o){
        if (!(o instanceof Node))
            return false;
        else{
            Node node = (Node) o;
            return this.hashCode() == node.hashCode();
        }      
    }
    
    @Override
    public String toString(){
        return this.cell.getRow()+","+this.cell.getCol();
    }

    public void addEdges(List<Edge> edges) {
        this.edges = edges;
    }
    
    public Node getPreviousNode(){
        return this.previousNode;
    }
    public void setPreviousNode(Node node){
        this.previousNode = node;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }
    
    
}
