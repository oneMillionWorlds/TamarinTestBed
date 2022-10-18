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
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.AnalogActionState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.math.RotationalVelocity;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;
import com.onemillionworlds.tamarin.vrhands.grabbing.SnapToHandGrabControl;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * This demonstrates how the hand's velocity can be used (perhaps for throwing grenades)
 */
public class HandVelocityExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("HandVelocityExampleState");

    VRAppState vrAppState;
    ActionBasedOpenVrState openVr;
    VRHandsAppState vrHands;

    Vector3f stoneCentre = new Vector3f(0,0.75f, 9.4f);
    Vector3f stoneTarget = new Vector3f(3,1.5f, 6);

    float targetHalfSize = 0.5f;

    List<Geometry> stones = new ArrayList<>();

    public HandVelocityExampleState(){
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

        eliminateExcessiveStones();
        ensureStonesAvailableToThrow();
        exitIfHitTarget();
    }

    private void ensureStonesAvailableToThrow(){

        boolean needNewStones = stones.stream().noneMatch(s -> s.getWorldTranslation().distance(stoneCentre) < 2);

        if (needNewStones){
            for(int i = -2; i <= 2; i++){
                for(int j = 0; j < 2; j++){
                    Geometry stone = createStoneToPickUp(stoneCentre.add(i * 0.2f, j*0.2f, 0), j == 0);
                    rootNodeDelegate.attachChild(stone);
                    stones.add(stone);
                }
            }
        }
    }

    private void exitIfHitTarget(){
        if (stones.stream().anyMatch(s -> {
            Vector3f stoneLocation = s.getWorldTranslation();
            Vector3f relativePosition = stoneLocation.subtract(stoneTarget);
            return (Math.abs(relativePosition.x)<targetHalfSize && Math.abs(relativePosition.y)<targetHalfSize && Math.abs(relativePosition.z)<targetHalfSize);
        })){
            getStateManager().detach(this);
            getStateManager().attach(new MenuExampleState());
        }
    }

    private void eliminateExcessiveStones(){
        while (stones.size() > 20){
            stones.remove(0).removeFromParent();
        }
    }

    private void initialiseScene(){
        rootNodeDelegate.attachChild(BlockMovingExampleState.checkerboardFloor(getApplication().getAssetManager()));

        //a lemur UI with text explaining what to do
        Container lemurWindow = new Container();
        lemurWindow.setLocalScale(0.02f); //lemur defaults to 1 meter == 1 pixel (because that make sense for 2D, scale it down, so it's not huge in 3d)
        Label label = new Label("Try throwing 'stones', you can also apply spin to them. Try moving (joysticks) while holding a stone \n\nBe careful not to throw the controller though ;)\n\nRed stones are affected by gravity, grey ones aren't\n\nTo exit throw a stone at the red box");
        lemurWindow.addChild(label);
        lemurWindow.setLocalTranslation(-5,4,0);
        rootNodeDelegate.attachChild(lemurWindow);
        rootNodeDelegate.attachChild(exitBox());
    }

    /**
     * This forms a "stone" geometry that reacts to being released by accessing the hands velocity and moving
     * from then on
     * @return a Geometry that is a stone
     */
    private Geometry createStoneToPickUp(Vector3f location, boolean gravityAffected){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/stone.png"));

        if (gravityAffected){
            boxMat.setColor("Color", ColorRGBA.Red);
        }
        boxGeometry.setMaterial(boxMat);

        boxGeometry.setLocalTranslation(location);
        SnapToHandGrabControl grabAndThrowControl = new SnapToHandGrabControl(new Vector3f(0.025f,0,0), 0.05f){
            boolean physicsActivated = false;

            Vector3f velocity = new Vector3f();
            RotationalVelocity angularVelocity = new RotationalVelocity(new Vector3f());

            @Override
            protected void controlUpdate(float tpf){
                super.controlUpdate(tpf);

                if (this.getGrabbingHand().isPresent() || !physicsActivated){
                    return; //don't apply "physics" to objects being held, the hand will take care of it
                }

                Spatial spatial = getSpatial();
                if (spatial.getWorldTranslation().y<0){
                    spatial.removeFromParent();
                    stones.remove(spatial);
                }

                if (gravityAffected){
                    velocity.y-=9.8*tpf;
                }

                spatial.setLocalTranslation(spatial.getLocalTranslation().add(velocity.mult(tpf)));

                Quaternion deltaRotation = new Quaternion();
                deltaRotation.fromAngleAxis(angularVelocity.getAngularVelocity()*tpf, angularVelocity.getAxis());
                spatial.setLocalRotation(deltaRotation.mult(spatial.getLocalRotation()));
            }

            @Override
            public void onRelease(BoundHand handUnbindingFrom){
                super.onRelease(handUnbindingFrom);
                velocity = handUnbindingFrom.getVelocity_world();
                angularVelocity = handUnbindingFrom.getRotationalVelocity_world();
                physicsActivated = true;
            }
        };
        boxGeometry.addControl(grabAndThrowControl);

        return boxGeometry;
    }

    private Geometry exitBox(){
        Box box = new Box(targetHalfSize, targetHalfSize, targetHalfSize);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.Red);
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(stoneTarget);

        return boxGeometry;
    }

}
