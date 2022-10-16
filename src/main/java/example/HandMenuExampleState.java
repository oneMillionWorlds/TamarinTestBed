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
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.functions.handmenu.HandMenuFunction;
import com.onemillionworlds.tamarin.vrhands.functions.handmenu.MenuBranch;
import com.onemillionworlds.tamarin.vrhands.functions.handmenu.MenuItem;
import com.onemillionworlds.tamarin.vrhands.functions.handmenu.MenuLeaf;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;

import java.util.ArrayList;
import java.util.List;

public class HandMenuExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("HandMenuExampleState");
    VRHandsAppState vrHandsAppState;

    List<FunctionRegistration> activeFunctionRegistrations = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        initialiseScene();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        activeFunctionRegistrations.forEach(FunctionRegistration::endFunction);
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager()));
        vrHandsAppState = getStateManager().getState(VRHandsAppState.ID, VRHandsAppState.class);
        attachHandMenus();
    }

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

    private void attachHandMenus(){
        vrHandsAppState.getHandControls().forEach(hand -> {
            List<MenuItem<String>> topMenuItems = new ArrayList<>();
            topMenuItems.add(new MenuLeaf<>(colouredBox(ColorRGBA.Red), "Red Box"));
            topMenuItems.add(new MenuLeaf<>(colouredBox(ColorRGBA.Orange), "Blue Box"));
            topMenuItems.add(new MenuBranch<>(sphere(ColorRGBA.White), List.of()));
            topMenuItems.add(new MenuLeaf<>(colouredBox(ColorRGBA.Cyan), "Green Box"));

            HandMenuFunction<String> handMenuFunction = new HandMenuFunction<>(topMenuItems, this::acceptSelection, "/actions/main/in/openHandMenu");
            FunctionRegistration functionRegistration = hand.addFunction(handMenuFunction);
            activeFunctionRegistrations.add(functionRegistration);
        });

    }

    private void acceptSelection(String selection){

    }

    private Spatial sphere(ColorRGBA colour){
        Sphere box = new Sphere(10, 10, 0.05f);
        Geometry boxGeometry = new Geometry("sphere", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

    private Spatial colouredBox(ColorRGBA colour){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }
}
