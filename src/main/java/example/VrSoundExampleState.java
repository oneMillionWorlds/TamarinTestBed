package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.SnapToHandGrabControl;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

public class VrSoundExampleState extends BaseAppState{

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

        soundBox(new Vector3f(0.3f,1.1f, 9.6f));

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

    private void soundBox(Vector3f location){

        Node soundNode = new Node("soundNode");

        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.randomColor());
        boxGeometry.setMaterial(boxMat);
        soundNode.setLocalTranslation(location);
        SnapToHandGrabControl grabControl = new SnapToHandGrabControl(new Vector3f(0.025f,0,0), 0.05f);
        soundNode.addControl(grabControl);

        soundNode.attachChild(boxGeometry);

        AudioNode sound = new AudioNode(getApplication().getAssetManager(), "Sounds/ring.wav");
        soundNode.attachChild(sound);

        boxGeometry.addControl(new AbstractControl(){
            final float cycleTime = 5;
            final float vibrateTime = 2;
            float time = 0;

            boolean sounded = false;

            @Override
            protected void controlUpdate(float tpf){
                time+=tpf;

                if (time>2){
                    sounded = false;
                }
                if (time>cycleTime){
                    time = 0;
                }
                if (time<0.5 && !sounded){
                    sound.playInstance();
                    sounded = true;
                }

                float fallOff = 1-(time/vibrateTime);
                if (fallOff<0){
                    fallOff = 0;
                }
                float deviation = fallOff * 0.01f * FastMath.sin(50*time);

                boxGeometry.setLocalTranslation(new Vector3f(deviation, 0, deviation));
            }

            @Override protected void controlRender(RenderManager rm, ViewPort vp){}
        });

        rootNodeDelegate.attachChild(soundNode);
    }
}
