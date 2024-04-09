package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.ClimbingPointGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

/**
 * This demonstrates how the ClimbingPointGrabControl can be used to support climbing.
 * <p>
 * You may want to use a different (physics based) mechanism for climbing but this produces
 * simple climbing easily by marking any spatial that's usable for climbing with a
 * ClimbingPointGrabControl control
 */
public class ClimbingExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("ClimbingExampleState");

    XrAppState xrAppState;
    OpenXrActionState openXrActions;
    VRHandsAppState vrHands;

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    public ClimbingExampleState(){
    }

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        xrAppState = getState(XrAppState.ID, XrAppState.class);
        openXrActions = getState(OpenXrActionState.ID, OpenXrActionState.class);
        vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);

        vrHands.getHandControls().forEach(boundHand ->
                closeHandBindings.add(boundHand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate)));

        initialiseScene();
    }


    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();

        vrHands.forceTerminateClimbing();

        closeHandBindings.forEach(FunctionRegistration::endFunction);
        closeHandBindings.clear();
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

        if (xrAppState.getPlayerFeetPosition().y > 8){
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
