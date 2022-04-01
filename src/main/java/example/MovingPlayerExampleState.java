package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.grabbing.GrabEventControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

/**
 * This is not actually anything to do with tamarin, its core JME movement, but it is provided to demonstrate
 * functionality.
 */
public class MovingPlayerExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("BlockMovingExampleState");

    VRAppState vrAppState;
    ActionBasedOpenVrState openVr;
    VRHandsAppState vrHands;

    Geometry observerBox;

    public MovingPlayerExampleState(){
    }

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        vrAppState = getState(VRAppState.class);
        openVr = getState(ActionBasedOpenVrState.class);
        vrHands = getState(VRHandsAppState.class);
        initialiseScene();
    }

    private Node getObserver(){
        return (Node)vrAppState.getObserver();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        Node observer = getObserver();
        observerBox.removeFromParent();
        observer.setLocalTranslation(new Vector3f(0,0,10));
        observer.lookAt(new Vector3f(0,0,0), Vector3f.UNIT_Y );
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        //the observer is the origin on the VR space (that the player then walks about in)
        Node observer = getObserver();

        DigitalActionState leftAction = openVr.getDigitalActionState("/actions/main/in/turnLeft");
        if (leftAction.changed && leftAction.state){
            Quaternion currentRotation = getObserver().getLocalRotation();
            Quaternion leftTurn = new Quaternion();
            leftTurn.fromAngleAxis(0.2f*FastMath.PI, Vector3f.UNIT_Y);

            observer.setLocalRotation(leftTurn.mult(currentRotation));
        }

        DigitalActionState rightAction = openVr.getDigitalActionState("/actions/main/in/turnRight", null);
        if (rightAction.changed && rightAction.state){

            Quaternion currentRotation = getObserver().getLocalRotation();
            Quaternion leftTurn = new Quaternion();
            leftTurn.fromAngleAxis(-0.2f*FastMath.PI, Vector3f.UNIT_Y);

            observer.setLocalRotation(leftTurn.mult(currentRotation));
        }

        //although we have by default bound teleport to the left hand the player may have redefined it, so check both
        for(BoundHand boundHand : vrHands.getHandControls()){
            DigitalActionState teleportAction = openVr.getDigitalActionState("/actions/main/in/teleport", boundHand.getHandSide().restrictToInputString);
            if (teleportAction.changed && teleportAction.state){
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
        AnalogActionState analogActionState = openVr.getAnalogActionState("/actions/main/in/walk");
        //we'll want the joystick to move the player relative to the head face direction, not the hand pointing direction
        Vector3f walkingDirectionRaw = new Vector3f(-analogActionState.x, 0, analogActionState.y);

        Vector3f playerRelativeWalkDirection = vrAppState.getVRViewManager().getLeftCamera().getRotation().mult(walkingDirectionRaw);
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

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Use the left hat forward to teleport forwards, left and right to sharply turn\n\nUse the right hat forward to walk (very nausea inducing)\n\nIn a real game you'd want some indicator to show where you were teleporting to. \n\nThe turn left and right is to help with seated experiences where physically turning 360 may be annoying or impossible. \n\n Teleport off the checkerboard to exit");
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
