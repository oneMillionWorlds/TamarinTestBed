package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
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
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.ParentRelativeMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictToGlobalLine;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictToLocalBox;
import com.onemillionworlds.tamarin.vrhands.grabbing.restrictions.RestrictToLocalLine;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

public class AdvancedRestrictedBoxGrabMovement extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        getState(VRHandsAppState.ID, VRHandsAppState.class).getHandControls().forEach(boundHand ->
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

        localBoxOnLine( new Vector3f(-0.3f,0.8f, 9.5f), new Vector3f(-0.3f,1.2f, 9.6f));

        exitBox(new Vector3f(-0.5f,1f, 10f));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("This example shows more advanced grab movement restriction, including box restriction and completely custom 'gear stick' restriction");
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

    private void localBoxOnLine(Vector3f lineMinPosition, Vector3f lineMaxPosition){
        Geometry lineGeometry = line(lineMinPosition, lineMaxPosition, ColorRGBA.Red);
        rootNodeDelegate.attachChild(lineGeometry);

        Node firstMovableNode = new Node("firstMovableNode");
        Geometry firstHandle = box(ColorRGBA.Blue, 0.05f);
        firstMovableNode.attachChild(firstHandle);
        firstMovableNode.setLocalTranslation(lineMinPosition);

        ParentRelativeMovingGrabControl firstGrabControl = new ParentRelativeMovingGrabControl(firstMovableNode);
        firstGrabControl.setGrabMoveRestriction(new RestrictToGlobalLine(lineMinPosition, lineMaxPosition));
        firstHandle.addControl(firstGrabControl);

        Vector3f localSecondRegionMin = new Vector3f(0.01f,0.01f,0.01f);
        Vector3f localSecondRegionMax = new Vector3f(0.3f,0.3f,0.3f);
        firstMovableNode.attachChild(containerBox(localSecondRegionMin, localSecondRegionMax, ColorRGBA.Blue));

        Geometry secondHandle = box(ColorRGBA.Red, 0.05f);
        secondHandle.setLocalTranslation(localSecondRegionMax);

        firstMovableNode.attachChild(secondHandle);
        RelativeMovingGrabControl secondGrabControl = new RelativeMovingGrabControl();
        secondGrabControl.setGrabMoveRestriction(new RestrictToLocalBox(localSecondRegionMin, localSecondRegionMax));
        secondHandle.addControl(secondGrabControl);
        rootNodeDelegate.attachChild(firstMovableNode);

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
        lineGeometry.setMaterial(lineMat);
        return lineGeometry;
    }

    private Geometry containerBox(Vector3f localMin, Vector3f localMax, ColorRGBA colour){
        Box line = new Box(localMin, localMax);
        Geometry lineGeometry = new Geometry("box", line);
        Material lineMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", colour);
        lineMat.getAdditionalRenderState().setWireframe(true);
        lineMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        lineGeometry.setMaterial(lineMat);
        return lineGeometry;
    }
}
