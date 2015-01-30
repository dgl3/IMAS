package cat.urv.imas.agent.communication.auction;

import cat.urv.imas.map.Cell;

/**
 *
 * @author philipp
 */
public class Item implements java.io.Serializable {
    private final Cell position;
    private final int load;

    public Item(Cell position, int load) {
        this.position = position;
        this.load = load;
    }
    
    public int getLoad() {
        return load;
    }

    public Cell getPosition() {
        return position;
    }
}
