/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cat.urv.imas.graph;

import java.util.List;

/**
 *
 * @author dgl3
 */
public class Path {
    private int distance;
    private List<Node> path;

    public Path( List<Node> path) {
        this.distance = path.size();
        this.path = path;
    }

    public int getDistance() {
        return distance;
    }

    public List<Node> getPath() {
        return path;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setPath(List<Node> path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Path{" + "distance=" + distance + ", path=" + path + '}';
    }

}
