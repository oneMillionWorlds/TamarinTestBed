package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.PhysicalBindingInterpretation;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import example.actions.ActionHandles;

/**
 * This example makes heavy use of the observer, see <a href="https://github.com/oneMillionWorlds/Tamarin/wiki/Understanding-the-observer">Understanding-the-observer</a>
 * for more details
 */
public class MovingPlayerExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("PlayerMovingExampleState");

    XrAppState xrAppState;
    OpenXrActionState openXrActionState;
    VRHandsAppState vrHands;

    Geometry observerBox;

    public MovingPlayerExampleState(){
    }

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        xrAppState = getState(XrAppState.ID, XrAppState.class);
        openXrActionState = getState(OpenXrActionState.ID, OpenXrActionState.class);
        vrHands = getState(VRHandsAppState.ID, VRHandsAppState.class);
        initialiseScene();
    }

    private Node getObserver(){
        return xrAppState.getObserver();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        observerBox.removeFromParent();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    @SuppressWarnings("DuplicatedCode") //as the examples are supposed to be self-contained accept some duplication
    @Override
    public void update(float tpf){

        super.update(tpf);

        //this is a temporary workaround until LWJGL is upgraded to 3.3.3, and we can use true dpads
        //syntheticDPad.updateRawAction(getStateManager().getState(OpenXrActionState.class).getVector2fActionState(ActionHandles.SYNTHETIC_D_PAD));

        //the observer is the origin on the VR space (that the player then walks about in)
        Node observer = getObserver();

        BooleanActionState leftAction = openXrActionState.getBooleanActionState(ActionHandles.TURN_LEFT); //temporary work around till LWJGL 3.3.3
        if (leftAction.hasChanged() && leftAction.getState()){
            xrAppState.rotateObserverWithoutMovingPlayer(-0.2f*FastMath.PI);
        }

        BooleanActionState rightAction = openXrActionState.getBooleanActionState(ActionHandles.TURN_RIGHT);
        if (rightAction.hasChanged() && rightAction.getState()){
            xrAppState.rotateObserverWithoutMovingPlayer(0.2f*FastMath.PI);
        }

        BooleanActionState backAction = openXrActionState.getBooleanActionState(ActionHandles.RESET_POSITION);
        if (backAction.hasChanged() && backAction.getState()){
            xrAppState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        }

        //Even though this is by default bound to the left hand, consider it against both the left and right hand in case its redefined by the player
        for(BoundHand boundHand : vrHands.getHandControls()){
            BooleanActionState teleportAction = boundHand.getBooleanActionState(ActionHandles.TELEPORT);
            if (teleportAction.hasChanged() && teleportAction.getState()){
                //teleport in the direction the hand that requested it is pointing
                Vector3f pointingDirection = boundHand.getBulkPointingDirection();
                pointingDirection.y=0;
                observer.setLocalTranslation(observer.getWorldTranslation().add(pointingDirection.mult(2)));

                Vector3f observerLocation = observer.getWorldTranslation();
                if (observerLocation.x < -5 || observerLocation.x > 5 ||  observerLocation.z < 5 || observerLocation.z > 15 ){
                    getStateManager().detach(this);
                    getStateManager().attach(new MenuExampleState());
                }

            }
        }


        //nausea inducing but nonetheless popular. Normal walking about
        Vector2fActionState analogActionState = openXrActionState.getVector2fActionState(ActionHandles.WALK);
        //we'll want the joystick to move the player relative to the head face direction, not the hand pointing direction
        Vector3f walkingDirectionRaw = new Vector3f(-analogActionState.getX(), 0, analogActionState.getY());

        Vector3f playerRelativeWalkDirection = xrAppState.getLeftCamera().getRotation().mult(walkingDirectionRaw);
        playerRelativeWalkDirection.y = 0;
        if (playerRelativeWalkDirection.length()>0.01){
            playerRelativeWalkDirection.normalizeLocal();
        }
        observer.setLocalTranslation(observer.getWorldTranslation().add(playerRelativeWalkDirection.mult(2f*tpf)));

    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(BlockMovingExampleState.checkerboardFloor(getApplication().getAssetManager()));

        //add some pillars just to add visual references
        pillar(-4.5f,5.5f, ColorRGBA.Red);
        pillar(4.5f,5.5f, ColorRGBA.Black);
        pillar(-4.5f,14.5f, ColorRGBA.Yellow);
        pillar(4.5f,14.5f, ColorRGBA.Blue);

        smallPillar(0,10, ColorRGBA.Orange);

        /*a lemur UI with text explaining what to do.
        * Note that it dynamically produces some of the tutorial text based on what actions are bound to what buttons
        * (Dpad stuff it harder, so is hard coded
        */
        PhysicalBindingInterpretation walkBinding = openXrActionState.getPhysicalBindingForAction(ActionHandles.WALK).get(0);
        String walkText = "Use " + walkBinding.handSide().map(h -> h.name().toLowerCase()).orElse("") + " " + walkBinding.fundamentalButton() +  " to walk (very nausea inducing)";

        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Use the left hat forward to teleport forwards, left and right to sharply turn. \n\nLeft hat backwards to teleport to (0,0,10) which is marked by the small orange pillar\n\nIn a real game you'd want some indicator to show where you were teleporting to. \n\n" + walkText + "\n\nThe turn left and right is to help with seated experiences where physically turning 360 may be annoying or impossible. \n\n Teleport off the checkerboard to exit");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);

        observerBox = observerBox();

        getObserver().attachChild(observerBox);
    }

    private void pillar(float x, float z, ColorRGBA colorRGBA){
        float pillarHeight = 2;
        Box pillar = new Box(0.25f, pillarHeight/2,  0.25f);
        Geometry pillarGeometry = new Geometry("pillar", pillar);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colorRGBA);
        pillarGeometry.setMaterial(boxMat);
        pillarGeometry.setLocalTranslation(new Vector3f(x, pillarHeight/2, z));
        rootNodeDelegate.attachChild(pillarGeometry);
    }

    private void smallPillar(float x, float z, ColorRGBA colorRGBA){
        float pillarHeight = 0.2f;
        Box pillar = new Box(0.05f, 0.2f,  0.05f);
        Geometry pillarGeometry = new Geometry("pillar", pillar);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colorRGBA);
        pillarGeometry.setMaterial(boxMat);
        pillarGeometry.setLocalTranslation(new Vector3f(x, pillarHeight/2, z));
        rootNodeDelegate.attachChild(pillarGeometry);
    }

    private Geometry observerBox(){
        Box box = new Box(0.10f, 0.10f, 0.10f);
        Geometry boxGeometry = new Geometry("box", box);
        Texture exitTexture = getApplication().getAssetManager().loadTexture("Textures/observer.png");
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", exitTexture);
        boxGeometry.setMaterial(boxMat);

        return boxGeometry;
    }
}
