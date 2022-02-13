package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.lemursupport.LemurKeyboard;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.bundledkeyboards.SimpleQwertyStyle;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.MouseListener;

/**
 * This app state gives a basic menu, implemented in lemur that can be interacted with via either hand
 */
public class KeyboardTestState extends BaseAppState{

    Node rootNodeDelegate = new Node("KeyboardExampleState");
    ActionBasedOpenVrState openVr;
    VRHandsAppState vrHands;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        openVr = getState(ActionBasedOpenVrState.class);
        vrHands = getState(VRHandsAppState.class);

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("Example of a keyboard based text entry"));

        lemurWindow.setLocalTranslation(-0.5f,1,6);

        rootNodeDelegate.attachChild(lemurWindow);

        //get the left hand and add a pick line to it
        vrHands.getHandControls().forEach(h -> {
            h.attachPickLine(pickLine());
            h.setPickMarkerContinuous(rootNodeDelegate);
        });

        getStateManager().attach(new LemurKeyboard(
                (key) -> System.out.println(key),
                (event,obj) -> System.out.println(event +":" +obj),
                "/actions/main/in/trigger",
                new SimpleQwertyStyle(),
                2,
                true
        ));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        vrHands.getHandControls().forEach(hand -> {
            if (openVr.getAnalogActionState("/actions/main/in/trigger", hand.getHandSide().restrictToInputString).x>0.5){
                hand.click_lemurSupport(rootNodeDelegate);
            }
        });
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        vrHands.getHandControls().forEach(boundHand -> {
            boundHand.clearPickMarkerContinuous();
            boundHand.removePickLine();
        });

    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

    private Spatial pickLine(){
        float length = 1;

        Cylinder pickCylinder = new Cylinder(10,10, 0.002f, length, true);

        Geometry geometry = new Geometry("debugHandLine", pickCylinder);
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.4f,0.4f,0.4f,0.5f));
        geometry.setMaterial(material);

        Quaternion rotate = new Quaternion();
        rotate.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        geometry.setLocalRotation(rotate);
        geometry.setLocalTranslation(length/2, 0,0);
        geometry.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geometry;
    }
}
