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
import com.jme3.scene.shape.Torus;
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
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HandMenuExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("HandMenuExampleState");
    VRHandsAppState vrHandsAppState;

    List<FunctionRegistration> activeFunctionRegistrations = new ArrayList<>();

    Label lastSelectedLabel = new Label("");

    private static final String PINK_MONKEY = "Pink Monkey";

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
        addExplanationBoard();
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
            topMenuItems.add(new MenuBranch<>(monkey(ColorRGBA.White), List.of(
                    new MenuLeaf<>(monkey(ColorRGBA.Green), "Green Monkey"),
                    new MenuLeaf<>(monkey(ColorRGBA.Gray), "Gray Monkey"),
                    new MenuLeaf<>(monkey(ColorRGBA.Pink), PINK_MONKEY),
                    new MenuLeaf<>(monkey(ColorRGBA.Orange), "Orange Monkey"),
                    new MenuLeaf<>(monkey(ColorRGBA.Red), "Red Monkey"))));

            topMenuItems.add(
                    new MenuBranch<>(curvedShapeCategory(), List.of(
                        new MenuBranch<>(sphereCategory(),  List.of(
                                new MenuLeaf<>(sphere(ColorRGBA.Red), "Red Sphere"),
                                new MenuLeaf<>(sphere(ColorRGBA.Blue), "Blue Sphere"),
                                new MenuLeaf<>(sphere(ColorRGBA.Gray), "Gray Sphere"),
                                new MenuLeaf<>(sphere(ColorRGBA.White), "White Sphere")
                        )),
                        new MenuBranch<>(torusCategory(), List.of(
                                new MenuLeaf<>(torus(ColorRGBA.Red), "Red Torus"),
                                new MenuLeaf<>(torus(ColorRGBA.Blue), "Blue Torus"),
                                new MenuLeaf<>(torus(ColorRGBA.Gray), "Gray Torus"),
                                new MenuLeaf<>(torus(ColorRGBA.White), "White Torus")
                        ))
                    ))
            );

            topMenuItems.add(new MenuLeaf<>(colouredBox(ColorRGBA.Cyan), "Cyan Box"));

            HandMenuFunction<String> handMenuFunction = new HandMenuFunction<>(topMenuItems, this::acceptSelection, "/actions/main/in/openHandMenu");
            FunctionRegistration functionRegistration = hand.addFunction(handMenuFunction);
            activeFunctionRegistrations.add(functionRegistration);
        });

    }

    private void addExplanationBoard(){
        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Press and hold the joystick (or dpad UP on Vive) on either hand\n\nMove the hand to the item you want to select (Some items have subcategories).\n\nSelect the pink monkey to exit");

        lemurWindow.addChild(label);
        lemurWindow.addChild(lastSelectedLabel);
        lemurWindow.setLocalTranslation(-5,4,0);
        acceptSelection(Optional.empty());
        rootNodeDelegate.attachChild(lemurWindow);

    }

    private void acceptSelection(Optional<String> selection){
        lastSelectedLabel.setText("Last Selected was: " + selection.orElse("Nothing"));

        if (selection.orElse("").equals(PINK_MONKEY)){
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        }
    }

    private Spatial curvedShapeCategory(){
        return cascade(sphere(ColorRGBA.Red), sphere(ColorRGBA.Blue), torus(ColorRGBA.Gray), torus(ColorRGBA.White));
    }

    private Spatial sphereCategory(){
        return cascade(sphere(ColorRGBA.Red), sphere(ColorRGBA.Blue), sphere(ColorRGBA.Gray), sphere(ColorRGBA.White));
    }

    private Spatial torusCategory(){
        return cascade(torus(ColorRGBA.Red), torus(ColorRGBA.Blue), torus(ColorRGBA.Gray), torus(ColorRGBA.White));
    }

    private Spatial sphere(ColorRGBA colour){
        Sphere box = new Sphere(20, 20, 0.05f);
        Geometry boxGeometry = new Geometry("sphere", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

    private Spatial torus(ColorRGBA colour){
        Torus box = new Torus(20, 20, 0.01f, 0.04f);
        Geometry boxGeometry = new Geometry("sphere", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

    private Spatial monkey(ColorRGBA colour){

        Spatial monkey = getApplication().getAssetManager().loadModel("Models/MonkeyHead/MonkeyHead.mesh.xml");
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        monkey.setLocalScale(0.05f);
        monkey.setMaterial(boxMat);
        return monkey;
    }

    private Spatial colouredBox(ColorRGBA colour){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

    /**
     * Makes the items smaller, and cascades them, intended for categories of items
     */
    private Node cascade(Spatial... itemsToCascade){
        float delta = 0.025f;

        Node centre = new Node();

        float offset = -itemsToCascade.length/2f* delta;
        for(Spatial item : itemsToCascade){
            item.setLocalScale(0.6f);
            item.setLocalTranslation(offset, offset, offset);
            offset+=delta;
            centre.attachChild(item);
        }
        return centre;
    }
}
