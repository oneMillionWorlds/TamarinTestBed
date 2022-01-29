package example;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.VRConstants;
import com.jme3.app.VREnvironment;
import com.jme3.app.state.AppState;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.HandSide;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;

import java.io.File;

public class Main extends SimpleApplication{

    BoundHand boundHandLeft;
    BoundHand boundHandRight;

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.put(VRConstants.SETTING_VRAPI, VRConstants.SETTING_VRAPI_OPENVR_LWJGL_VALUE);
        VREnvironment env = new VREnvironment(settings);
        env.initialize();
        if (env.isInitialized()){
            VRAppState vrAppState = new VRAppState(settings, env);

            Main app = new Main(vrAppState);
            app.setLostFocusBehavior(LostFocusBehavior.Disabled);
            app.setSettings(settings);
            app.setShowSettings(false);
            app.start();
        }
    }

    public Main(AppState... appStates) {
        super(appStates);
    }

    @Override
    public void simpleInitApp(){
        ActionBasedOpenVrState actionBasedOpenVrState = new ActionBasedOpenVrState();
        getStateManager().attach(actionBasedOpenVrState);

        File actionManifestLocation = new File("openVr/actionManifest.json");

        actionBasedOpenVrState.registerActionManifest(actionManifestLocation.getAbsolutePath(), "/actions/main" );

        initialiseHands();
        initialiseScene();
    }

    @Override
    public void simpleUpdate(float tpf){
        super.simpleUpdate(tpf);

        VRAppState vrAppState = getStateManager().getState(VRAppState.class);
        vrAppState.getLeftViewPort().setBackgroundColor(ColorRGBA.Brown);
        vrAppState.getRightViewPort().setBackgroundColor(ColorRGBA.Brown);

        ActionBasedOpenVrState openVr = getStateManager().getState(ActionBasedOpenVrState.class);

        if (openVr.getAnalogActionState("/actions/main/in/trigger", null).x>0.5){
            CollisionResults pick = boundHandLeft.pickBulkHand(rootNode);

            boundHandLeft.click_lemurSupport(rootNode);
            int a=0;
        }

    }

    private void initialiseHands(){
        VRHandsAppState vrHandsAppState = new VRHandsAppState(assetManager, getStateManager().getState(ActionBasedOpenVrState.class));
        getStateManager().attach(vrHandsAppState);

        Spatial handLeft =assetManager.loadModel("Tamarin/Models/basicHands_left.j3o");
        boundHandLeft = vrHandsAppState.bindHandModel("/actions/main/in/HandPoseLeft", "/actions/main/in/HandSkeletonLeft", handLeft, HandSide.LEFT);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Tamarin/Textures/basicHands_left_referenceTexture.png"));

        boundHandLeft.setMaterial(mat);
        boundHandLeft.setGrabAction("/actions/main/in/grip", rootNode);

        Spatial rightHand =assetManager.loadModel("Tamarin/Models/basicHands_right.j3o");

        boundHandRight= vrHandsAppState.bindHandModel("/actions/main/in/HandPoseRight", "/actions/main/in/HandSkeletonRight", rightHand, HandSide.RIGHT);
        boundHandRight.setGrabAction("/actions/main/in/grip", rootNode);

        Material matRight = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRight.setTexture("ColorMap", assetManager.loadTexture("Tamarin/Textures/basicHands_right_referenceTexture.png"));
        boundHandRight.setMaterial(matRight);
    }

    private void initialiseScene(){
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
        rootNode.attachChild(floor);

        grabbableBox(new Vector3f(0,1f, 9.5f));
        grabbableBox(new Vector3f(0.2f,1.2f, 9.5f));
        grabbableBox(new Vector3f(-0.2f,0.9f, 9.5f));
        grabbableBox(new Vector3f(0.3f,1.1f, 9.6f));
    }

    private void grabbableBox(Vector3f location){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.randomColor());
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        AutoMovingGrabControl grabControl = new AutoMovingGrabControl(new Vector3f(0.025f,0,0), 0.05f);
        boxGeometry.addControl(grabControl);
        rootNode.attachChild(boxGeometry);
    }
}
