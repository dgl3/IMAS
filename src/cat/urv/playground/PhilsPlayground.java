/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.playground;

import cat.urv.imas.gui.CellVisualizer;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.map.StreetCell;

/**
 *
 * @author philipp
 */
public class PhilsPlayground {
    public static void main(String args[]){
        Cell cell = new StreetCell(1, 2);
        System.out.println(cell.getCol());
    }
}
