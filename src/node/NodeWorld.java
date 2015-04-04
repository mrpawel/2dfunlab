package node;

import mytree.MyTree;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by user on 3/24/2015.
 */
public class NodeWorld {
    private final static double maxNodeSize = 32; //diameter
    public final static Rectangle myBounds = new Rectangle(0,0,1024,800);
    private final static MyTree myTree = new MyTree(myBounds, maxNodeSize);

    public static CopyOnWriteArrayList<Node> nodes = new CopyOnWriteArrayList<Node>();
    public static int totalNodes = 1000;

    public static void buildWorld(){
        ArrayList<Node> tempNodes = new ArrayList<Node>();
        for(int i=0; i<totalNodes; i++){
            tempNodes.add(new Node(Math.random()*512 + 256, Math.random()*512 + 256, maxNodeSize, myTree));
        }
        nodes = new CopyOnWriteArrayList<Node>(tempNodes);
    }

    public static void getAverageNeighbors(){
        float total=0;
        for(Node node : nodes){
            total+=node.neighbors.size();
        }
        //System.out.println("AV NEIGHBS " + total/nodes.diameter());
    }

    public static void drawNodes(Graphics2D g){
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //anti aliasing on
        //myTree.drawGrid(g);
        for(Node node : nodes){
            node.draw(g);
        }
    }

    public static void update(double x, double y){

        myTree.clear();
        for(Node node : nodes) {
            node.updatePos();
            myTree.insert(node);
        }
        for(Node node : nodes) {
            node.updateNeighbors();
        }

        //getAverageNeighbors();

        if(nodes.size()>0){
            nodes.get(0).setPos(x-20,y-20);
        }

    }
}
