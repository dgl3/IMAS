package cat.urv.imas.agent.communication;

import cat.urv.imas.map.Cell;

/**
 *
 * @author philipp
 */
public class AmbulanceDetails  implements java.io.Serializable {
    private final Cell position;
    private final int load;

    public AmbulanceDetails(Cell position, int load) {
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
