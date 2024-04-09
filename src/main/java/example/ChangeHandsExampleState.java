package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

public class ChangeHandsExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    VRHandsAppState handsAppState;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        handsAppState = getState(VRHandsAppState.ID, VRHandsAppState.class);
        handsAppState.getHandControls().forEach(boundHand ->
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

        Material devilHandMaterial = new Material(getApplication().getAssetManager(),"Common/MatDefs/Light/Lighting.j3md");
        devilHandMaterial.setTexture("DiffuseMap", getApplication().getAssetManager().loadTexture("Textures/demonHands.png"));

        grabbableHand(new Vector3f(-0.1f,1f, 9.5f), "Models/demonHandsLeft.glb", devilHandMaterial);

        Material pinStripeMaterial = new Material(getApplication().getAssetManager(),"Common/MatDefs/Light/Lighting.j3md");
        pinStripeMaterial.setTexture("DiffuseMap", getApplication().getAssetManager().loadTexture(BoundHand.DEFAULT_HAND_TEXTURE));

        grabbableHand(new Vector3f(0.1f,1f, 9.5f), BoundHand.DEFAULT_HAND_MODEL_LEFT, pinStripeMaterial);

        exitBox(new Vector3f(-0.5f,1f, 9.6f));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("This example is used to test changing hands at runtime. A devil hand can be grabbed to swap to it (left hand only)");
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

    private void grabbableHand(Vector3f location, String handModelPath, Material material){

        Spatial handModel = getApplication().getAssetManager().loadModel(handModelPath);
        handModel.setMaterial(material);

        handModel.setLocalTranslation(location);

        handModel.addControl(new GrabEventControl(() -> {
            BoundHand boundHand = handsAppState.getHandControl(HandSide.LEFT).orElseThrow();
            boundHand.updateHandGeometryAndControlledArmature(VRHandsAppState.searchForArmatured(handModel.clone()));
        }));

        rootNodeDelegate.attachChild(handModel);

    }
}
