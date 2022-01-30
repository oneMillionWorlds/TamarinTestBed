package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.event.MouseListener;

/**
 * This is not actually anything to do with tamarin, its core JME movement, but it is provided to demonstrate
 * functionality.
 */
public class MovingPlayerExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    ActionBasedOpenVrState openVr;

    public MovingPlayerExampleState(){
    }

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        openVr = getState(ActionBasedOpenVrState.class);
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

    @Override
    public void update(float tpf){
        super.update(tpf);

        DigitalActionState leftAction = openVr.getDigitalActionState("/actions/main/in/turnLeft", null);
        if (leftAction.changed && leftAction.state){
            //the "camera" is not the real camera, it is acting as the origin on the VR space (that the player then walks about in)
            Quaternion currentRotation = getApplication().getCamera().getRotation();
            Quaternion leftTurn = new Quaternion();
            leftTurn.fromAngleAxis(0.2f*FastMath.PI, Vector3f.UNIT_Y);

            getApplication().getCamera().setRotation(leftTurn.mult(currentRotation));
        }

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(BlockMovingExampleState.checkerboardFloor(getApplication().getAssetManager()));

        //add some pillars just to add visual references
        pillar(-4.5f,5.5f, ColorRGBA.Red);
        pillar(4.5f,5.5f, ColorRGBA.Black);
        pillar(-4.5f,14.5f, ColorRGBA.Yellow);
        pillar(4.5f,14.5f, ColorRGBA.Blue);

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.01f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Use the left hat forward to teleport forwards, left and right to sharply turn\n\nUse the right hat forward to walk (very nausea inducing)\n\nIn a real game you'd want some indicator to show where you were teleporting to. \n\nThe turn left and right is to help with seated experiences where physically turning 360 may be annoying or impossible");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(0,5,0);
        rootNodeDelegate.attachChild(lemurWindow);
    }

    private void pillar(float x, float z, ColorRGBA colorRGBA){
        float pillarHeight = 2;
        Box pillar = new Box(0.25f, pillarHeight/2,  0.25f);
        Geometry pillarGeometry = new Geometry("pillar", pillar);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colorRGBA);
        pillarGeometry.setMaterial(boxMat);
        pillarGeometry.setLocalTranslation(new Vector3f(x, pillarHeight/2, z));
        rootNodeDelegate.attachChild(pillarGeometry);
    }

}
