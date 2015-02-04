package graph.test;

import cat.urv.imas.graph.Edge;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.Node;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
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
    private GameSettings game;
    private Graph graph;
    
    private GameSettings gameEval;
    private Graph graphEval;
    

    @Before 
    public void initialize() {
        game = InitialGameSettings.load("game.settings");
        graph = new Graph(game);
        
        gameEval = InitialGameSettings.load("game2.settings");
        graphEval = new Graph(gameEval);
    }
    
    @Test
    public void getNodesTest(){
        /**
         * StreetCell (2,2) has 4 neighbours
         */
        Node node = graph.getNodes().get(new StreetCell(2,2));
        List<Edge> edges = node.getEdges();
        assertEquals(4,edges.size());
    }
    
    @Test
    public void BFS1Test(){
        //(1,2) a (6,2), path should be equal to [(1,2),(2,2),(3,2),(4,2),(5,2),(6,2)]
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(6,2);
        Path path = graph.bfs(initialPoint, finalPoint, 18);
        assertTrue(path.getDistance() == 5);
    }
    
    @Test
    public void BFS2Test(){
        /**(1,2) a (3,9), size path = 10  **/
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(3,9);
        Path path = graph.bfs(initialPoint, finalPoint, 18);
        assertTrue(path.getDistance() == 9);
    }
    
    
    @Test
    public void BFS3Test(){ //Should return null
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(15,9);
        Path path = graph.bfs(initialPoint, finalPoint, 18);
        assertEquals(path,null);
    }
    
    
    @Test
    public void strangeErrorWhenDistanceEqual2Test(){
        Cell initialPoint = new StreetCell(1,2);
        Cell finalPoint = new StreetCell(1,4);
        Path path = graph.bfs(initialPoint, finalPoint, 18);
        assertTrue(path != null);
        assertTrue(path.getDistance() == 2);
    }
    
    @Test
    public void strangeError2AffectingCells_2_18_and_3_17Test(){
        Cell initialPoint = new StreetCell(2,18);
        Cell finalPoint = new StreetCell(3,17);
        Path path = graph.computeOptimumPath(initialPoint, finalPoint,18);
        assertTrue(path != null);
    }
    
    @Test
    public void strangeError3AffectingCells_14_17_and_16_11Test(){
        Cell initialPoint = new StreetCell(14,17);
        Cell finalPoint = new StreetCell(16,11);
        Path path = graph.computeOptimumPath(initialPoint, finalPoint,18);
        assertTrue(path == null);        
    }
    @Test
    public void strangeError4AffectingCells_1_7_and_16_11Test(){
        Cell initialPoint = new StreetCell(1,7);
        Cell finalPoint = new StreetCell(16,11);
        Path path = graph.computeOptimumPath(initialPoint, finalPoint,18);
        assertTrue(path != null);   
    }

    
    @Test
    public void getAdjacentCellsMethodTest(){
        //Should return only 1 adjacent cell        
        Cell targetCell = new BuildingCell(10,0,0);
        List<Cell> adjacentCells = graph.getAdjacentCells(targetCell);
        assertTrue(adjacentCells.size() == 1);
        
        //Should return only 2 adjacent cell
        targetCell = new BuildingCell(10,0,1);
        adjacentCells = graph.getAdjacentCells(targetCell);
        assertTrue(adjacentCells.size() == 2);
        
        
        //Should return only 3 adjacent cell
        targetCell = new BuildingCell(10,0,2);
        adjacentCells = graph.getAdjacentCells(targetCell);
        assertTrue(adjacentCells.size() == 3);
        
        //Should return only 5 adjacent cell
        targetCell = new BuildingCell(10,3,3);
        adjacentCells = graph.getAdjacentCells(targetCell);
        assertTrue(adjacentCells.size() == 5);
    }
    
    @Test
    public void computeOptimumPathMethodTest(){
        Cell initialPoint = new StreetCell(1,2);
        Cell targetCell = new BuildingCell(10,3,3);
        Path path = graph.computeOptimumPath(initialPoint, targetCell,18);
        assertTrue(path.getDistance() == 1);
    }
    
    @Test
    public void computeOptimumPathWithRestrictionsMethodTest(){
        Cell initialPoint = new StreetCell(1,1);
        Cell targetCell = new BuildingCell(0,5,3);
        Cell restrictedCell = new StreetCell(1,2);
        Path path = graph.computeOptimumPathWithRestrictions(initialPoint, targetCell, restrictedCell, 18);
        assertNotNull(path);
        assertTrue(!path.getPath().contains(new Node(restrictedCell)));   
    }
    
    @Test
    public void initGameSettings2(){
        assertTrue(this.gameEval != null);
        assertTrue(this.graphEval != null);
        
        //What's going on with ambulances
        Cell[][] map = gameEval.getMap();
        int rows = map.length;
        int cols = map[0].length;
        Path path = null;
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++){
                if(map[i][j].getCellType().equals(CellType.STREET)){
                    System.out.println("Computing from: ["+i+","+j+"]");
                    for(int i2=0;i2<rows;i2++){
                        for(int j2=0;j2<cols;j2++){
                            if(map[i2][j2].getCellType().equals(CellType.STREET)){
                                if(!map[i][j].equals(map[i2][j2])){
                                    System.out.println("to ["+i2+","+j2+"]");
                                    path = this.graphEval.bfs(new StreetCell(i,j), new StreetCell(i2,j2), Integer.MAX_VALUE);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        
        //Path path = this.graphEval.bfs(new StreetCell(13,5), new StreetCell(15,21), Integer.MAX_VALUE);
        assertTrue(path != null);
    }
    @Test
    public void initGameSettings1(){
        assertTrue(this.game != null);
        assertTrue(this.graph != null);
        
        //What's going on with ambulances
        Cell[][] map = game.getMap();
        int rows = map.length;
        int cols = map[0].length;
        Path path = null;
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++){
                if(map[i][j].getCellType().equals(CellType.STREET)){
                    System.out.println("Computing from: ["+i+","+j+"]");
                    for(int i2=0;i2<rows;i2++){
                        for(int j2=0;j2<cols;j2++){
                            if(map[i2][j2].getCellType().equals(CellType.STREET)){
                                if(!map[i][j].equals(map[i2][j2])){
                                    System.out.println("to ["+i2+","+j2+"]");
                                    path = this.graph.bfs(new StreetCell(i,j), new StreetCell(i2,j2), Integer.MAX_VALUE);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        
        //Path path = this.graphEval.bfs(new StreetCell(13,5), new StreetCell(15,21), Integer.MAX_VALUE);
        assertTrue(path != null);
    }
    
    
    @Test
    public void initGameSettingsSpecific(){
        assertTrue(this.gameEval != null);
        assertTrue(this.graphEval != null);
        
        //What's going on with ambulances
        Path path = this.graph.bfs(new StreetCell(1,2), new StreetCell(1,6), Integer.MAX_VALUE);
        assertTrue(path != null);
    }
    
    

}
