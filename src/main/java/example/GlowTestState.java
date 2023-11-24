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
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.RelativeMovingGrabControl;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlowTestState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    Collection<Runnable> removeGlowPostProcessor = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        getState(VRHandsAppState.ID, VRHandsAppState.class).getHandControls().forEach(boundHand ->
                closeHandBindings.add(boundHand.setGrabAction(ActionHandles.GRIP, rootNodeDelegate)));

        getState(XrAppState.ID, XrAppState.class).configureBothViewports(viewPort -> {
            AssetManager assetManager = getApplication().getAssetManager();
            FilterPostProcessor filterPostProcessor = buildPostProcessors(assetManager);
            viewPort.addProcessor(filterPostProcessor);
            removeGlowPostProcessor.add(() -> viewPort.removeProcessor(filterPostProcessor));
        });

        initialiseScene();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        closeHandBindings.forEach(FunctionRegistration::endFunction);
        closeHandBindings.clear();
        removeGlowPostProcessor.forEach(Runnable::run);
        removeGlowPostProcessor.clear();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager()));

        glowBox(new Vector3f(0f,1.1f, 9.6f), ColorRGBA.Green, true);
        glowBox(new Vector3f(0f,1.3f, 9.6f), ColorRGBA.Green, false);

        glowBox(new Vector3f(-0.3f,1.1f, 9.6f), ColorRGBA.Blue, true);
        glowBox(new Vector3f(-0.3f,1.3f, 9.6f), ColorRGBA.Blue, false);

        glowBox(new Vector3f(0.3f,1.1f, 9.6f), ColorRGBA.White, true);
        glowBox(new Vector3f(0.3f,1.3f, 9.6f), ColorRGBA.White, false);

        exitBox(new Vector3f(-0.5f,1f, 9.6f));
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

    private void glowBox(Vector3f location, ColorRGBA colour, boolean glow){
        Geometry boxGeometry = box(colour, glow);
        boxGeometry.setLocalTranslation(location);
        RelativeMovingGrabControl relativeGrabControl = new RelativeMovingGrabControl();
        boxGeometry.addControl(relativeGrabControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

    private Geometry box(ColorRGBA colour, boolean glow){
        Box mainBox = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("parent", mainBox);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        if(glow){
            boxMat.setColor("GlowColor", colour);
        }

        boxGeometry.setMaterial(boxMat);

        return boxGeometry;
    }

    private static FilterPostProcessor buildPostProcessors(AssetManager assetManager){
        FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
        BloomFilter bf=new BloomFilter(BloomFilter.GlowMode.Objects);
        bf.setBlurScale(0.5f);
        fpp.addFilter(bf);

        return fpp;
    }
}
