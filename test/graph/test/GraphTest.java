package graph.test;

import cat.urv.imas.graph.Edge;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.Node;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.InitialGameSettings;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dgl3
 * 
 * JUnit test used for testing the graph search algorithms
 * 
 */
public class GraphTest {
    
    private Graph graph;
    

    @Before 
    public void initialize() {
        GameSettings game = InitialGameSettings.load("game.settings");
        graph = new Graph(game);
    }
    
    @Test
    public void testPathFromXtoY(){
        /**
         * StreetCell (2,2) has 4 neighbours
         */
        Node node = graph.getNodes().get(new StreetCell(2,2));
        List<Edge> edges = node.getEdges();
        assertEquals(4,edges.size());
    }
    
    @Test
    public void testBFS1(){
        //(1,2) a (6,2), path should be equal to [(1,2),(2,2),(3,2),(4,2),(5,2),(6,2)]
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(6,2);
        Path path = graph.bfs(initialPoint, finalPoint);
        assertTrue(path.getDistance() == 5);
    }
    
    @Test
    public void testBFS2(){
        /**(1,2) a (3,9), size path = 10  **/
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(3,9);
        Path path = graph.bfs(initialPoint, finalPoint);
        assertTrue(path.getDistance() == 9);
    }
    
    
    @Test
    public void testBFS3(){ //Should return null
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(15,9);
        Path path = graph.bfs(initialPoint, finalPoint);
        assertEquals(path,null);
    }

}
