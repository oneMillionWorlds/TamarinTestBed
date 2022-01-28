package example;

import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

public class Test{

    public static void main(String[] args){
        Node parent = new Node("parent");
        Node mid = new Node("mid");
        Node child = new Node("child");
        parent.attachChild(mid);
        mid.attachChild(child);


    }
}
