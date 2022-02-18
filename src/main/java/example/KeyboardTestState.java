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
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.VersionedList;
import com.simsilica.lemur.event.MouseListener;

import java.util.List;

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

        addPrimaryKeyboard();
        addSecondaryKeyboard();

        //get the left hand and add a pick line to it
        vrHands.getHandControls().forEach(h -> {
            h.attachPickLine(pickLine());
            h.setPickMarkerContinuous(rootNodeDelegate);
            h.setClickAction_lemurSupport("/actions/main/in/trigger", rootNodeDelegate);
        });
    }


    private void addPrimaryKeyboard(){
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("Example of a keyboard based text entry\nThe below is a text field, click into it to start typing"));
        TextField textField = lemurWindow.addChild(new TextField(""));
        lemurWindow.addChild(new Label("Type `Exit` to exit"));

        textField.getControl(GuiControl.class).addListener(new GuiControlListener(){
            @Override public void reshape(GuiControl source, Vector3f pos, Vector3f size){}
            @Override public void focusGained(GuiControl source){}

            @Override
            public void focusLost(GuiControl source){
                if (textField.getText().trim().equalsIgnoreCase("exit")){
                    getStateManager().detach(KeyboardTestState.this);
                    getStateManager().attach(new MenuExampleState());
                }
            }
        });

        lemurWindow.setLocalTranslation(-0.5f,1,6);

        rootNodeDelegate.attachChild(lemurWindow);
    }

    private void addSecondaryKeyboard(){
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.005f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        lemurWindow.addChild(new Label("This form is for testing the behaviour of multiple text fields and the keyboard"));
        lemurWindow.addChild(new TextField(""));
        lemurWindow.addChild(new Label("There is a second text field"));
        lemurWindow.addChild(new TextField(""));
        lemurWindow.addChild(new Label("This form is also not at the default rotation\nSo also tests that that works ok"));
        lemurWindow.addChild(new Checkbox("Example Checkbox"));
        lemurWindow.addChild(new Label("Example ListBox (partial support only):"));
        VersionedList<String> dropDownExamples = new VersionedList<>(List.of("Alpha","Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa"));
        lemurWindow.addChild(new ListBox<>(dropDownExamples));

        lemurWindow.setLocalTranslation(2,1,7);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(-FastMath.QUARTER_PI, Vector3f.UNIT_Y);
        lemurWindow.setLocalRotation(rotation);

        rootNodeDelegate.attachChild(lemurWindow);
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        vrHands.getHandControls().forEach(boundHand -> {
            boundHand.clearPickMarkerContinuous();
            boundHand.removePickLine();
            boundHand.clearClickAction_lemurSupport();
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
