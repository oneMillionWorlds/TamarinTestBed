package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.math.RotationalVelocity;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.ClimbingPointGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * This demonstrates how the hand's velocity can be used (perhaps for throwing grenades)
 */
public class ClimbingExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("ClimbingExampleState");

    VRAppState vrAppState;
    ActionBasedOpenVrState openVr;
    VRHandsAppState vrHands;

    public ClimbingExampleState(){
    }

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        vrAppState = getState(VRAppState.class);
        openVr = getState(ActionBasedOpenVrState.class);
        vrHands = getState(VRHandsAppState.class);
        initialiseScene();
    }

    private Node getObserver(){
        return (Node)vrAppState.getObserver();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        Node observer = getObserver();
        vrHands.forceTerminateClimbing();
        observer.setLocalTranslation(new Vector3f(0,0,10));
        observer.lookAt(new Vector3f(0,0,0), Vector3f.UNIT_Y );
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
        //the observer is the origin on the VR space (that the player then walks about in)
        Node observer = getObserver();

        DigitalActionState leftAction = openVr.getDigitalActionState("/actions/main/in/turnLeft");
        if (leftAction.changed && leftAction.state){
            Quaternion currentRotation = getObserver().getLocalRotation();
            Quaternion leftTurn = new Quaternion();
            leftTurn.fromAngleAxis(0.2f*FastMath.PI, Vector3f.UNIT_Y);

            observer.setLocalRotation(leftTurn.mult(currentRotation));
        }

        DigitalActionState rightAction = openVr.getDigitalActionState("/actions/main/in/turnRight", null);
        if (rightAction.changed && rightAction.state){

            Quaternion currentRotation = getObserver().getLocalRotation();
            Quaternion leftTurn = new Quaternion();
            leftTurn.fromAngleAxis(-0.2f*FastMath.PI, Vector3f.UNIT_Y);

            observer.setLocalRotation(leftTurn.mult(currentRotation));
        }

        //although we have by default bound teleport to the left hand the player may have redefined it, so check both
        for(BoundHand boundHand : vrHands.getHandControls()){
            DigitalActionState teleportAction = openVr.getDigitalActionState("/actions/main/in/teleport", boundHand.getHandSide().restrictToInputString);
            if (teleportAction.changed && teleportAction.state){
                //teleport in the direction the hand that requested it is pointing
                Vector3f pointingDirection = boundHand.getBulkPointingDirection();
                pointingDirection.y=0;
                observer.setLocalTranslation(observer.getWorldTranslation().add(pointingDirection.mult(2)));
            }
        }

        //nausea inducing but nonetheless popular. Normal walking about
        AnalogActionState analogActionState = openVr.getAnalogActionState("/actions/main/in/walk");
        //we'll want the joystick to move the player relative to the head face direction, not the hand pointing direction
        Vector3f walkingDirectionRaw = new Vector3f(-analogActionState.x, 0, analogActionState.y);

        Vector3f playerRelativeWalkDirection = vrAppState.getVRViewManager().getLeftCamera().getRotation().mult(walkingDirectionRaw);
        playerRelativeWalkDirection.y = 0;
        if (playerRelativeWalkDirection.length()>0.01){
            playerRelativeWalkDirection.normalizeLocal();
        }
        observer.setLocalTranslation(observer.getWorldTranslation().add(playerRelativeWalkDirection.mult(2f*tpf)));

        if (observer.getWorldTranslation().y > 8){
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        }
    }


    private void initialiseScene(){
        rootNodeDelegate.attachChild(BlockMovingExampleState.checkerboardFloor(getApplication().getAssetManager()));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Try climbing the wall (on your left) by grabbing the red handholds.\n\nTo exit climb to the top");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);
        addClimbable();
    }

    private void addClimbable(){

        //a main (unclimbable) backboard in grey
        Geometry backboard = backboard(new Vector3f(-1, 5, 10), new Vector3f(0.1f, 5, 0.5f));
        rootNodeDelegate.attachChild(backboard);

        //climbable hand holds
        for(float y = 0.5f; y<5; y+=0.4f){
            rootNodeDelegate.attachChild(handHold(new Vector3f(-1, y, 9.8f)));
            rootNodeDelegate.attachChild(handHold(new Vector3f(-1, y+0.2f, 10.2f)));
        }

        //a bulk climbable region (to test that a single geometry is fine having 2 hands holding it
        Geometry largeClimbableRegion = createBox(new Vector3f(-1, 7, 10), new Vector3f(0.2f, 2, 0.3f), ColorRGBA.Red);
        largeClimbableRegion.addControl(new ClimbingPointGrabControl());
        rootNodeDelegate.attachChild(largeClimbableRegion);
    }

    private Geometry handHold(Vector3f locationCentre){
        Geometry handHold = createBox(locationCentre, new Vector3f(0.2f, 0.05f, 0.05f), ColorRGBA.Red);
        handHold.addControl(new ClimbingPointGrabControl());
        return handHold;
    }

    private Geometry createBox(Vector3f locationCentre, Vector3f size,  ColorRGBA colour){
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);

        boxGeometry.setLocalTranslation(locationCentre);
        return boxGeometry;
    }
    private Geometry backboard(Vector3f locationCentre, Vector3f size){
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));
        boxGeometry.setMaterial(boxMat);

        boxGeometry.setLocalTranslation(locationCentre);
        return boxGeometry;
    }
}
