/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.test;

import cat.urv.imas.graph.Edge;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.GraphUtils;
import cat.urv.imas.graph.Node;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.InitialGameSettings;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dgl3
 */
public class GraphUtilsTest {

    private Graph graph; 
    private GameSettings game;
    
    @Before 
    public void initialize() {
        this.game = InitialGameSettings.load("game.settings");
        this.graph = new Graph(this.game);
    }
    
    @Test
    public void actionAreaTest() throws Exception{
        Cell cell = new StreetCell(1,2);
        int maxNumActionArea = 18;
        Graph actionAreaGraph = GraphUtils.actionArea(this.graph, cell, maxNumActionArea);
        
        Node currentNode = actionAreaGraph.getNodes().get(cell);        
        assertTrue(currentNode.getNumActionArea() == maxNumActionArea);

        List<Edge> edges = currentNode.getEdges();
        for(Edge edge:edges){
            Node child = actionAreaGraph.getNodes().get(edge.getNode2().getCell());
            assertTrue(child.getNumActionArea() == 17);
        }
        
    }
    
}
