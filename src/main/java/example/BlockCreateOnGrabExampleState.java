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
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.functions.GrabPickingFunction;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

/**
 * This demonstrates how a grab control can be added to an already clenched hand.
 */
@SuppressWarnings("DuplicatedCode")
public class BlockCreateOnGrabExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockCreateOnGrabExampleState");

    VRHandsAppState vrHandsAppState;

    OpenXrActionState openXrActionState;

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        vrHandsAppState = getStateManager().getState(VRHandsAppState.ID, VRHandsAppState.class);
        openXrActionState = getStateManager().getState(OpenXrActionState.ID, OpenXrActionState.class);

        vrHandsAppState.getHandControls().forEach(boundHand ->
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
        Label label = new Label("Every time you clench the hand a new box will be produced in it. \n\n Try clenching and placing some blocks");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);

        exitBox(new Vector3f(-0.5f,1f, 9.6f));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        vrHandsAppState.getHandControls().forEach(hand -> {

            if (hand.getFloatActionState(ActionHandles.GRIP).getState()>0.6f){
                //see if the hand is already grabbing anything, if not magic up a new block and give it to the hand
                GrabPickingFunction grabFunction = hand.getFunction(GrabPickingFunction.class);
                if (!grabFunction.isCurrentlyHoldingSomething()){
                    //use the hand's palm node to decide where to place new box
                    RelativeMovingGrabControl newControl = grabbableBox(hand.getPalmNode().getWorldTranslation(), hand.getPalmNode().getWorldRotation());
                    grabFunction.manuallyGiveControlToHold(newControl);
                }
            }
        });
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

    private RelativeMovingGrabControl grabbableBox(Vector3f location, Quaternion startingRotation){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.randomColor());
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        boxGeometry.setLocalRotation(startingRotation);
        RelativeMovingGrabControl grabControl = new RelativeMovingGrabControl();
        boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);
        return grabControl;
    }
}
