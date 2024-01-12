package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.Haptic;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.touching.ButtonMovementAxis;
import com.onemillionworlds.tamarin.vrhands.touching.MechanicalButton;
import example.actions.ActionHandles;

/**
 * This shows non lemur buttons that respond to the player touching them with the finger.
 */
public class MechanicalButtonExample extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);

        Node panelCenter = new Node("PanelCenter");
        panelCenter.setLocalTranslation(0, 1f, 0);
        rootNodeDelegate.attachChild(panelCenter);
        panelCenter.attachChild(app.getAssetManager().loadModel("Models/buttonPanel.j3o"));
        panelCenter.setLocalRotation(new Quaternion().fromAngleAxis(-0.2f*FastMath.PI, Vector3f.UNIT_X));


        Node nonToggleButtons = createNonToggleButtons();
        nonToggleButtons.setLocalTranslation(0.2f, -0.1f, 0.05f);
        panelCenter.attachChild(nonToggleButtons);

        XrAppState xrAppState = getState(XrAppState.ID, XrAppState.class);
        if(xrAppState!=null){
            xrAppState.movePlayersFeetToPosition(new Vector3f(0, 0, 0.75f));
            xrAppState.playerLookAtPosition(new Vector3f(0, 0, 0));
            VRHandsAppState vrHandsAppState = getState(VRHandsAppState.ID, VRHandsAppState.class);
            vrHandsAppState.getHandControls().forEach(handControl -> {
                handControl.setFingerTipPressDetection(rootNodeDelegate, false, ActionHandles.HAPTIC, 0.25f);
            });
        }else{
            //this is tested for its non VR behaviour as well, so we need to set the camera position
            Camera c = getApplication().getCamera();
            c.setLocation(new Vector3f(0, 1.5f, 3));
            c.lookAt(new Vector3f(0, 1f, 0), Vector3f.UNIT_Y);
        }

    }


    private Node createNonToggleButtons(){

        Node start = new Node();

        int displayIndex = 0;

        boolean red = true;
        for(int i=0;i<2;i++){
            for(int j=0;j<2;j++){
                red = !red;

                Geometry button = (Geometry)getApplication().getAssetManager().loadModel(red ? "Models/buttons/redCircle.j3o" : "Models/buttons/blueCircle.j3o").clone();

                MechanicalButton mechanicalButton = new MechanicalButton(button, ButtonMovementAxis.NEGATIVE_Z, 0.02f, 0.5f);
                mechanicalButton.setHapticOnFullDepress(new Haptic(ActionHandles.HAPTIC, 0.1f, 100f, 0.5f));
                start.attachChild(mechanicalButton);

                mechanicalButton.setLocalTranslation(i*0.35f, j*0.25f, 0);

                DisplayLight displayLight = buildDisplayLight(red ? ColorRGBA.Red : new ColorRGBA(0.5f, 0.5f, 1, 1), true);

                mechanicalButton.addPressListener(() -> {
                    displayLight.setDisplayIntensity(1);
                });

                displayLight.light.setLocalTranslation(displayIndex*0.035f, 0.45f, 0);
                start.attachChild(displayLight.light);

                displayIndex++;
            }
        }
        return start;
    }


    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}

    private DisplayLight buildDisplayLight(ColorRGBA displayColour, boolean autoFade){
        Geometry simpleBox = new Geometry("Box", new Box(0.01f, 0.01f, 0.01f));
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
