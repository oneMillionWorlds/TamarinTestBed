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
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.HandSpec;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;
import example.actions.ActionSets;

import java.util.ArrayList;
import java.util.List;

/**
 * This state tests detaching and reattaching the three XR states
 */
public class DetachAndReattachExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockCreateOnGrabExampleState");

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    boolean reattachCountDown = false;
    float reattachTimer;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        getStateManager().getState(VRHandsAppState.ID, VRHandsAppState.class).getHandControls().forEach(boundHand ->
                closeHandBindings.add(boundHand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate)));

        initialiseScene();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        closeHandBindings.forEach(FunctionRegistration::endFunction);
        closeHandBindings.clear();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager()));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("This tests that all the XR states can be detached and reattached. \nGrab the box and the states will detach for 5 seconds then reattach. VR should exit and restart");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);

        exitBox(new Vector3f(-0.5f,1f, 9.6f));

        detachAndReattachBox(new Vector3f(0.5f,1f, 9.6f));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        if (reattachCountDown){
            reattachTimer -= tpf;
            if (reattachTimer < 0){
                reattachCountDown = false;

                HandSpec handSpec = Main.handSpec().toBuilder()
                        .postBindLeft(hand -> hand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate))
                        .postBindRight(hand -> hand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate))
                        .build();
                XrAppState xrAppState = new XrAppState();
                xrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
                xrAppState.setMainViewportConfiguration(v -> v.setBackgroundColor(ColorRGBA.Brown));
                getStateManager().attach(xrAppState);
                getStateManager().attach(new OpenXrActionState(Main.manifest(), ActionSets.MAIN));
                getStateManager().attach(new VRHandsAppState(handSpec));

                getStateManager().getState(VRHandsAppState.ID, VRHandsAppState.class).getHandControls().forEach(boundHand ->
                        closeHandBindings.add(boundHand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate)));
            }
        }
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

    private void detachAndReattachBox(Vector3f location){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Texture exitTexture = getApplication().getAssetManager().loadTexture("Textures/detachReattach.png");
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", exitTexture);
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        GrabEventControl grabControl = new GrabEventControl(() -> {
            getStateManager().detach(getState(XrAppState.ID, XrAppState.class));
            getStateManager().detach(getState(OpenXrActionState.ID, OpenXrActionState.class));
            getStateManager().detach(getState(VRHandsAppState.ID, VRHandsAppState.class));

            reattachTimer = 5;
            reattachCountDown = true;
        });
        boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

}
