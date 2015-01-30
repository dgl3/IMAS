
package cat.urv.imas.graph;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represent connections between nodes
 * @author dgl3
 */
public class Edge implements Serializable{
    
    private Node node1;
    private Node node2;
    
    public Edge(Node node1, Node node2){
        this.node1 = node1;
        this.node2 = node2;
    }
    
    /**
     * Returns the actual node
     * @return Node
     */
    public Node getNode1(){
        return node1;
    }
    
    /**
     * Returns the neighbour node
     * @return Node
     */
    public Node getNode2(){
        return node2;
    }
    
    @Override
    public boolean equals(Object o){
        if (!(o instanceof Edge))
            return false;
        else{
            Edge edge = (Edge) o;
            return (this.node1.hashCode() == edge.getNode1().hashCode() &&
                    this.node2.hashCode() == edge.getNode2().hashCode()) 
                    || (this.node1.hashCode() == edge.getNode2().hashCode() &&
                    this.node2.hashCode() == edge.getNode1().hashCode());
        }        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.node1);
        hash = 53 * hash + Objects.hashCode(this.node2);
        return hash;
    }
    
    @Override
    public String toString(){
        return "Edge from "+this.node1.toString()+" to "+this.node2.toString();
        
    }
    

    
}
