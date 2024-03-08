package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.Haptic;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.onemillionworlds.tamarin.vrhands.touching.ButtonMovementAxis;
import com.onemillionworlds.tamarin.vrhands.touching.MechanicalButton;
import com.onemillionworlds.tamarin.vrhands.touching.MechanicalToggle;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

/**
 * This shows non lemur buttons that respond to the player touching them with the finger.
 */
public class MechanicalButtonExample extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    List<FunctionRegistration> closeHandBindings = new ArrayList<>();

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        Node panelCenter = new Node("PanelCenter");

        panelCenter.setLocalScale(0.5f); //so I built this too big, scaling down the whole thing to avoid recalculating the positions of everything

        panelCenter.setLocalTranslation(0, 1f, 0);
        rootNodeDelegate.attachChild(panelCenter);
        panelCenter.attachChild(app.getAssetManager().loadModel("Models/buttonPanel.j3o"));
        panelCenter.setLocalRotation(new Quaternion().fromAngleAxis(-0.2f*FastMath.PI, Vector3f.UNIT_X));


        Node nonToggleButtons = createNonToggleButtons();
        nonToggleButtons.setLocalTranslation(0.2f, -0.1f, 0.05f);
        panelCenter.attachChild(nonToggleButtons);

        Node toggleButtons = createToggleButtons();
        toggleButtons.setLocalTranslation(-0.4f, -0.1f, 0.07f);
        panelCenter.attachChild(toggleButtons);

        XrAppState xrAppState = getState(XrAppState.ID, XrAppState.class);

        xrAppState.movePlayersFeetToPosition(new Vector3f(0, 0, 0.75f));
        xrAppState.playerLookAtPosition(new Vector3f(0, 0, 0));
        VRHandsAppState vrHandsAppState = getState(VRHandsAppState.ID, VRHandsAppState.class);
        vrHandsAppState.getHandControls().forEach(handControl -> {
            closeHandBindings.add(handControl.setFingerTipPressDetection(rootNodeDelegate, false, ActionHandles.HAPTIC, 0.25f));
            closeHandBindings.add(handControl.setGrabAction(ActionHandles.GRIP, rootNodeDelegate));

            closeHandBindings.add(handControl.setPickMarkerContinuous(rootNodeDelegate));
            closeHandBindings.add(handControl.setClickAction_lemurSupport(ActionHandles.TRIGGER, rootNodeDelegate));
        });

        exitBox(new Vector3f(-0.5f, 1f, 0.5f));
    }

    private Node createNonToggleButtons(){

        Node start = new Node();

        int displayIndex = 0;

        boolean red = true;
        for(int i=0;i<2;i++){
            for(int j=0;j<2;j++){
                red = !red;

                Geometry button = (Geometry)getApplication().getAssetManager().loadModel(red ? "Models/buttons/redCircle.j3o" : "Models/buttons/blueCircle.j3o");

                MechanicalButton mechanicalButton = new MechanicalButton(button, ButtonMovementAxis.NEGATIVE_Z, 0.02f, 0.5f);
                mechanicalButton.setHapticOnFullDepress(new Haptic(ActionHandles.HAPTIC, 0.1f, 100f, 0.5f));
                start.attachChild(mechanicalButton);

                mechanicalButton.setLocalTranslation(i*0.35f, j*0.25f, 0);

                DisplayLight displayLight = buildDisplayLight(red ? ColorRGBA.Red : new ColorRGBA(0.5f, 0.5f, 1, 1), true);

                mechanicalButton.addPressListener(() -> displayLight.setDisplayIntensity(1));

                displayLight.light.setLocalTranslation(displayIndex*0.06f, 0.45f, 0);
                start.attachChild(displayLight.light);

                displayIndex++;
            }
        }
        return start;
    }

    private Node createToggleButtons(){

        Node start = new Node();

        int displayIndex = 0;

        List<MechanicalToggle> toggleList = new ArrayList<>();

        boolean red = true;
        for(int i=0;i<2;i++){
            for(int j=0;j<2;j++){
                red = !red;

                Geometry button = (Geometry)getApplication().getAssetManager().loadModel(red ? "Models/buttons/redHexagon.j3o" : "Models/buttons/blueHexagon.j3o").clone();

                MechanicalToggle mechanicalToggle = new MechanicalToggle(button, ButtonMovementAxis.NEGATIVE_Z, 0.04f, 0.03f, 0.2f);
                toggleList.add(mechanicalToggle);
                mechanicalToggle.setHapticOnFullDepress(new Haptic(ActionHandles.HAPTIC, 0.1f, 100f, 0.5f));
                start.attachChild(mechanicalToggle);

                mechanicalToggle.setLocalTranslation(i*0.25f, j*0.25f, 0);

                DisplayLight displayLight = buildDisplayLight(red ? ColorRGBA.Red : new ColorRGBA(0.5f, 0.5f, 1, 1), false);

                mechanicalToggle.addPressListener((toggleState) -> displayLight.setDisplayIntensity(toggleState.isAKindOfOn() ? 1 :0));

                displayLight.light.setLocalTranslation(displayIndex*0.06f, 0.45f, 0);
                start.attachChild(displayLight.light);

                displayIndex++;
            }
        }

        //reset button
        Geometry resetButtonGeometry = (Geometry)getApplication().getAssetManager().loadModel("Models/buttons/redCircle.j3o");
        MechanicalButton resetButton = new MechanicalButton(resetButtonGeometry, ButtonMovementAxis.NEGATIVE_Z, 0.02f, 0.5f);
        resetButton.setLocalTranslation(-0.3f, 0.25f, -0.02f);
        start.attachChild(resetButton);
        resetButton.addPressListener(() -> toggleList.forEach(t -> t.setState(MechanicalToggle.ToggleState.TRANSITIONING_OFF)));

        return start;
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

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        closeHandBindings.forEach(FunctionRegistration::endFunction);
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}

    private DisplayLight buildDisplayLight(ColorRGBA displayColour, boolean autoFade){
        Geometry simpleBox = new Geometry("Box", new Box(0.02f, 0.02f, 0.02f));
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        simpleBox.setMaterial(material);

        ColourControl colourControl = new ColourControl(material, displayColour, autoFade);

        simpleBox.addControl(colourControl);

        return new DisplayLight(simpleBox, colourControl);
    }

    private record DisplayLight(Spatial light, ColourControl control) {

        public void setDisplayIntensity(float intensity){
            control.setDisplayIntensity(intensity);
        }
    }

    private static class ColourControl extends AbstractControl{
        float intensity = 0;
        boolean autoFade;
        ColorRGBA baseColour;
        Material materialBeingControlled;

        public ColourControl(Material materialBeingControlled, ColorRGBA baseColour, boolean autoFade){
            this.materialBeingControlled = materialBeingControlled;
            this.autoFade = autoFade;
            this.baseColour = baseColour;
            setDisplayIntensity(0);
        }

        @Override
        protected void controlUpdate(float tpf){
            if (autoFade && intensity>0){
                setDisplayIntensity(Math.max(0, intensity-tpf));
            }
        }

        private void setDisplayIntensity(float intensity){
            this.intensity = intensity;
            materialBeingControlled.setColor("Color", baseColour.mult(0.25f+0.75f*intensity));

        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp){}
    }

}
