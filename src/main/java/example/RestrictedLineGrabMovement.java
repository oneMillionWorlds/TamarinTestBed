package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.ParentRelativeMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import java.util.List;

public class RestrictedLineGrabMovement extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        initialiseScene();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager()));

        grabbableBoxOnLine(new Vector3f(0,1f, 9.5f), new Vector3f(0,0.8f, 9.5f), new Vector3f(0,1.2f, 9.5f), false, ColorRGBA.Blue);
        grabbableBoxOnLine(new Vector3f(0.3f,1.1f, 9.6f), new Vector3f(-0.1f,1.1f, 9.6f), new Vector3f(0.6f,1.1f, 9.6f), true, ColorRGBA.Red);
        grabbablePointsOnParent(new Vector3f(-0.4f, 0.9f, 9.7f ));
        chainedParent(new Vector3f(0.5f, 0.9f, 9.9f));

        exitBox(new Vector3f(-0.5f,1f, 10f));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("This example shows advanced uses of grab controls\n\nThe boxes on lines show how grab controls can restrict the motion of control being controlled by grabbing\nThe blue box is constrained to the line and does not rotated\nThe red box is also constrained to the line, but is allowed to rotate.\n\nThe large object with pink grab points can only be moved using the grab points but the whole item moves together");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);
    }

    @SuppressWarnings("DuplicatedCode") //each example is supposed to be mostly stand along so allow some duplication
    public static Geometry checkerboardFloor(AssetManager assetManager){
        Quad floorQuad = new Quad(10,10);
        Geometry floor = new Geometry("floor", floorQuad);
        Texture floorTexture = assetManager.loadTexture("Textures/checkerBoard.png");
        floorTexture.setMagFilter(Texture.MagFilter.Nearest);
        Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", floorTexture);

        floor.setMaterial(mat);
        Quaternion floorRotate = new Quaternion();
        floorRotate.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        floor.setLocalRotation(floorRotate);
        floor.setLocalTranslation(-5,0,15);

        return floor;
    }

    @SuppressWarnings("DuplicatedCode") //each example is supposed to be mostly stand along so allow some duplication
    private void exitBox(Vector3f location){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Texture exitTexture = getApplication().getAssetManager().loadTexture("Textures/grabToExit.png");
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", exitTexture);
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        GrabEventControl grabControl = new GrabEventControl(() -> {
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        });
        boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

    private void grabbableBoxOnLine(Vector3f location, Vector3f minPosition, Vector3f maxPosition, boolean canRotate, ColorRGBA colour){
        Geometry boxGeometry = box(colour, 0.05f);
        boxGeometry.setLocalTranslation(location);

        RelativeMovingGrabControl relativeGrabControl = new RelativeMovingGrabControl();
        relativeGrabControl.restrictToPath(minPosition, maxPosition);
        relativeGrabControl.setShouldApplyRotation(canRotate);

        boxGeometry.addControl(relativeGrabControl);
        rootNodeDelegate.attachChild(boxGeometry);

        Geometry lineGeometry = line(minPosition, maxPosition, colour);

        rootNodeDelegate.attachChild(lineGeometry);
    }

    /**
     * An example showing how the parented movements can be chained. Allowing grab motion on a larger entity that is
     * itself grab movable.
     * <p>
     * This also shows how the parent of a grabbable doesn't have to be the root node; it all just works itself out
     */
    private void chainedParent(Vector3f location){

        Node parent = new Node("chainedParent");
        parent.setLocalTranslation(location);

        parent.attachChild( line(new Vector3f(0,0,0), new Vector3f(0,0.3f,0), ColorRGBA.Orange));
        Geometry box1Geometry = box(ColorRGBA.Orange, 0.05f);

        Node node1 = new Node("node1");
        parent.attachChild(node1);
        node1.attachChild(box1Geometry);
        RelativeMovingGrabControl relative1 = new RelativeMovingGrabControl();
        relative1.restrictToPath(new Vector3f(0,0,0), new Vector3f(0,0.3f,0));
        node1.addControl(relative1);

        node1.attachChild( line(new Vector3f(0,0,0), new Vector3f(0.3f,0,0), ColorRGBA.White));
        Node node2a = new Node("node2b");
        Geometry box2aGeometry = box(ColorRGBA.White, 0.04f);
        RelativeMovingGrabControl relative2a = new RelativeMovingGrabControl();
        relative2a.restrictToPath(new Vector3f(0,0,0), new Vector3f(0.3f,0,0)); //note the way the path is relative to the parent
        node2a.setLocalTranslation(new Vector3f(0.3f,0,0));
        node2a.addControl(relative2a);
        node2a.attachChild(box2aGeometry);
        node1.attachChild(node2a);

        node2a.attachChild(line(new Vector3f(0,0,0), new Vector3f(0,0,0.2f), ColorRGBA.Cyan));
        Geometry box3Geometry = box(ColorRGBA.Cyan, 0.03f);
        RelativeMovingGrabControl relative3 = new RelativeMovingGrabControl();
        relative3.restrictToPath(new Vector3f(0,0,0), new Vector3f(0,0,0.2f));
        box3Geometry.setLocalTranslation(0,0,0.2f);
        box3Geometry.addControl(relative3);
        node2a.attachChild(box3Geometry);

        //a separate, simpler chain coming off the main cube
        node1.attachChild( line(new Vector3f(0,0,0), new Vector3f(-0.3f,0,0), ColorRGBA.Black));
        Geometry box2bGeometry = box(ColorRGBA.Black, 0.04f);
        RelativeMovingGrabControl relative2b = new RelativeMovingGrabControl();
        relative2b.restrictToPath(new Vector3f(0,0,0), new Vector3f(-0.3f,0,0));
        box2bGeometry.setLocalTranslation(new Vector3f(-0.3f,0,0));
        box2bGeometry.addControl(relative2b);
        node1.attachChild(box2bGeometry);

        rootNodeDelegate.attachChild(parent);
    }

    private void grabbablePointsOnParent(Vector3f location){
        Node parentNode = new Node();
        parentNode.setLocalTranslation(location);

        Geometry boxGeometry = box(ColorRGBA.Black, 0.15f);

        parentNode.attachChild(boxGeometry);

        for(float dx : List.of(-0.15f, 0.15f)){
            for(float dy : List.of(-0.15f, 0.15f)){
                for(float dz : List.of(-0.15f, 0.15f)){
                    Geometry grabGeometry = box(ColorRGBA.Pink, 0.03f);
                    grabGeometry.setLocalTranslation(new Vector3f(dx, dy, dz));
                    parentNode.attachChild(grabGeometry);
                    ParentRelativeMovingGrabControl parentRelativeMovingGrabControl = new ParentRelativeMovingGrabControl(parentNode);
                    grabGeometry.addControl(parentRelativeMovingGrabControl);
                }
            }
        }
        rootNodeDelegate.attachChild(parentNode);
    }

    private Geometry box(ColorRGBA colour, float halfSize){
        Box mainBox = new Box(halfSize, halfSize, halfSize);
        Geometry boxGeometry = new Geometry("parent", mainBox);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);

        return boxGeometry;
    }

    private Geometry line(Vector3f localMin, Vector3f localMax, ColorRGBA colour){
        Line line = new Line(localMin, localMax);
        Geometry lineGeometry = new Geometry("line", line);
        Material lineMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", colour);
        lineMat.getAdditionalRenderState().setLineWidth(10);
        lineGeometry.setMaterial(lineMat);
        return lineGeometry;
    }
}
