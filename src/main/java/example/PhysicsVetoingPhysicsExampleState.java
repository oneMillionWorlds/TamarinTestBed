package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.PhysicsSweepTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.ConvexShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vinette.VrVignetteState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.vrhands.functions.FunctionRegistration;
import example.actions.ActionHandles;

import java.util.ArrayList;
import java.util.List;

/**
 * This example has the whole player as an active physics object. This can be a little
 * unkind on the human player but some game styles require it
 */
public class PhysicsVetoingPhysicsExampleState extends BaseAppState{
    private static final float TOTAL_OCCLUSION_PENETRATION_DEPTH = 0.10f;

    private static final float MAXIMUM_ALLOWED_STEP_HEIGHT = 0.25f;

    private static final float FALL_CHECK_STEP_HEIGHT = 0.01f;

    private static final float PLAYER_FALL_SPEED = 10f;

    Node rootNodeDelegate = new Node("BlockMovingExampleState");
    XrAppState vrAppState;
    XrActionAppState openXrActionState;
    VRHandsAppState vrHands;

    BulletAppState bulletAppState;

    List<FunctionRegistration> functionRegistrations = new ArrayList<>();

    VrVignetteState vignette = new VrVignetteState();

    CollisionShape headCollisionShape = new SphereCollisionShape(0.3f);
    PhysicsGhostObject headGhostSphere = new PhysicsGhostObject(headCollisionShape);

    float headObjectPenetration_last = 0;
    float headObjectPenetration = 0;

    PhysicsSpace physicsSpace;

    boolean playerIsFalling = false;

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        vrAppState = getState(XrAppState.ID, XrAppState.class);
        openXrActionState = getState(XrActionAppState.ID, XrActionAppState.class);
        vrHands = getState(VRHandsAppState.class);
        vignette.setVignetteAmount(0);

        getStateManager().attach(vignette);

        bulletAppState = new BulletAppState();
        getStateManager().attach(bulletAppState);
        initialiseScene();

    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
        functionRegistrations.forEach(FunctionRegistration::endFunction);
        getStateManager().detach(vignette);
        getStateManager().detach(bulletAppState);
    }

    @Override
    protected void onEnable(){}

    @Override
    protected void onDisable(){}

    private void initialiseScene(){
        physicsSpace = bulletAppState.getPhysicsSpace();
        physicsSpace.setMaxTimeStep(1f/90);

        rootNodeDelegate.attachChild(checkerboardFloor(getApplication().getAssetManager(), physicsSpace));

        table(new Vector3f(2, 0.5f, 10), new Vector3f(0.25f, 0.1f, 0.25f), physicsSpace);

        grabbableBox(new Vector3f(2, 0.6f, 10), physicsSpace);
        grabbableBox(new Vector3f(2, 0.7f, 10), physicsSpace);
        grabbableBox(new Vector3f(2, 0.6f, 10.1f), physicsSpace);
        grabbableBox(new Vector3f(2.1f, 0.6f, 10), physicsSpace);

        wall(new Vector3f(-1, 5, 10), new Vector3f(0.1f, 5, 0.5f), physicsSpace);

        physicsSpace.add(headGhostSphere);

        physicsSpace.addTickListener(new PhysicsTickListener(){
            @Override
            public void prePhysicsTick(PhysicsSpace space, float timeStep){
                headObjectPenetration_last = headObjectPenetration;
                headObjectPenetration = 0;
            }

            @Override
            public void physicsTick(PhysicsSpace space, float timeStep){

            }
        });


        physicsSpace.addOngoingCollisionListener(event -> {
            if (event.getObjectA() == headGhostSphere || event.getObjectB() == headGhostSphere){
                System.out.println("Collision " + event.getDistance1());
                headObjectPenetration = Math.max(headObjectPenetration, Math.abs(event.getDistance1()));
            }
        });

        //lastTickPhysicsFeetPosition = playerControl.getPhysicsLocation().subtract(0, playersTrueCapsuleHeight/2,0);

        //add some stairs to walk up
        step(new Vector3f(-1,0, 5), new Vector3f(1,0.2f, 8), ColorRGBA.Red, physicsSpace);
        step(new Vector3f(-1,0.2f, 5), new Vector3f(1,0.4f, 7.5f), ColorRGBA.Blue, physicsSpace);
        step(new Vector3f(-1,0.4f, 5), new Vector3f(1,0.6f, 7), ColorRGBA.Green, physicsSpace);
        step(new Vector3f(-1,0.6f, 5), new Vector3f(1,0.8f, 6.5f), ColorRGBA.Red, physicsSpace);
        step(new Vector3f(-1,0.8f, 5), new Vector3f(1,1f, 6.0f), ColorRGBA.Blue, physicsSpace);
        step(new Vector3f(-1,1f, 5), new Vector3f(1,1.2f, 5.5f), ColorRGBA.Green, physicsSpace);

        getObserver().attachChild(debugBox(ColorRGBA.Red));

    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        Vector3f headPosition = vrAppState.getVrCameraPosition();

        moveViaControls(tpf);

        //the engine only moves the body if something went wrong(e.g. collisions, falling), this should lead to observer slip to correct observer to physics (rather than the normal other way round)
        //Vector3f currentPhysicsChange = physicsFeetLocation.subtract(lastTickPhysicsFeetPosition);
        //getObserver().setLocalTranslation(getObserver().getLocalTranslation().add(currentPhysicsChange));
        //vrFeetPosition.addLocal(currentPhysicsChange); //need to let the feet know they have been updated or else the normal correction tries to "uncorrect" it

        //this is the normal behaviour; physics follows the VR headset
        //where the vrFeetPosition and physicsFeetLocation are not aligned and this isn't due to a physics correction move the physics object to align them
        headGhostSphere.setPhysicsLocation(headPosition);

        vignette.setVignetteAmount(Math.clamp(headObjectPenetration_last/TOTAL_OCCLUSION_PENETRATION_DEPTH, 0, 1));

        if(playerIsFalling){
            fall(tpf);
        }

    }

    public void moveViaControls(float timeslice){

        Vector2fActionState analogActionState = openXrActionState.getVector2fActionState(ActionHandles.WALK);
        //we'll want the joystick to move the player relative to the head face direction, not the hand pointing direction
        Vector3f walkingDirectionRaw = new Vector3f(-analogActionState.getX(), 0, analogActionState.getY());

        Quaternion lookDirection = new Quaternion().lookAt(vrAppState.getVrCameraLookDirection(), Vector3f.UNIT_Y);

        Vector3f playerRelativeWalkDirection = lookDirection.mult(walkingDirectionRaw);
        playerRelativeWalkDirection.y = 0;
        if (playerRelativeWalkDirection.length()>0){
            playerRelativeWalkDirection.normalizeLocal();

            float sizeOfFootTest = 0.3f;
            ConvexShape footTestShape = new SphereCollisionShape(sizeOfFootTest);

            Vector3f startingFootPosition = getPlayerFeetPosition().add(0, MAXIMUM_ALLOWED_STEP_HEIGHT + sizeOfFootTest, 0);
            Vector3f endingFootPosition = startingFootPosition.add(playerRelativeWalkDirection.mult(2f * timeslice));

            Transform startingFootTransform = new Transform();
            startingFootTransform.setTranslation(startingFootPosition);

            Transform endingFootTransform = new Transform();
            endingFootTransform.setTranslation(endingFootPosition);

            List<PhysicsSweepTestResult> results = physicsSpace.sweepTest(footTestShape, startingFootTransform, endingFootTransform);
            if(results.isEmpty()){
                // allow the motion
                getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(playerRelativeWalkDirection.mult(2f * timeslice)));

                // see if we should now "step up" as a result of an incline or fall
                float totalTestLineLength = sizeOfFootTest + MAXIMUM_ALLOWED_STEP_HEIGHT + FALL_CHECK_STEP_HEIGHT;
                float bottomOfFootTestLineLength = sizeOfFootTest + MAXIMUM_ALLOWED_STEP_HEIGHT;

                List<PhysicsRayTestResult> physicsRayTestResults = physicsSpace.rayTest(endingFootPosition, endingFootPosition.add(0, -totalTestLineLength, 0));

                if(physicsRayTestResults.isEmpty()){
                    playerIsFalling = true;
                } else{
                    // see if we should "step up"
                    float furthestPointFraction = Float.MAX_VALUE;
                    for(PhysicsRayTestResult rayTestResult : physicsRayTestResults){
                        furthestPointFraction = Math.min(furthestPointFraction, rayTestResult.getHitFraction());
                    }
                    float furthestPointLength = furthestPointFraction * totalTestLineLength;
                    if(furthestPointLength < bottomOfFootTestLineLength){
                        float stepUp = bottomOfFootTestLineLength - furthestPointLength;
                        getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(0, stepUp, 0));
                    }
                }
            }
        }
    }

    public void fall(float timeslice){
        Vector3f playerFootPosition = getPlayerFeetPosition();

        float distanceToTest = 1;

        List<PhysicsRayTestResult> physicsRayTestResults = physicsSpace.rayTest(playerFootPosition, playerFootPosition.add(0, -distanceToTest, 0));

        float fractionToGround = Float.MAX_VALUE;
        for(PhysicsRayTestResult rayTestResult : physicsRayTestResults){
            fractionToGround = Math.min(fractionToGround, rayTestResult.getHitFraction());
        }
        float distanceToGround = fractionToGround * distanceToTest;

        float distanceToFall = timeslice * PLAYER_FALL_SPEED;
        if(distanceToFall>distanceToGround){
            playerIsFalling = false;
            distanceToFall = distanceToGround;
        }

        getObserver().setLocalTranslation(getObserver().getWorldTranslation().add(0, -distanceToFall, 0));
    }


    public static Geometry checkerboardFloor(AssetManager assetManager, PhysicsSpace physicsSpace){
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

        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        floor.addControl(rigidBodyControl);

        physicsSpace.addCollisionObject(rigidBodyControl);
        return floor;
    }


    /**
     * TODO; need hand physics and ability to pick up box
     * @param location
     * @param physicsSpace
     */
    private void grabbableBox(Vector3f location, PhysicsSpace physicsSpace){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.randomColor());
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        //AutoMovingGrabControl grabControl = new AutoMovingGrabControl(new Vector3f(0.025f,0,0), 0.05f);
        //boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);

        RigidBodyControl rigidBodyControl = new RigidBodyControl(1);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);
    }

    private void wall(Vector3f locationCentre, Vector3f size, PhysicsSpace physicsSpace){
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeometry = new Geometry("wall", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(locationCentre);
        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

    private void table(Vector3f locationCentre, Vector3f size, PhysicsSpace physicsSpace){
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeometry = new Geometry("wall", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(locationCentre);
        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }

    private void step(Vector3f min, Vector3f max, ColorRGBA colour, PhysicsSpace physicsSpace){
        Vector3f size = max.subtract(min);
        Box box = new Box(size.x/2, size.y/2, size.z/2);
        Geometry boxGeometry = new Geometry("physicsBox", box);
        boxGeometry.setLocalTranslation(min.add(max).multLocal(0.5f));
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colour);
        boxMat.setTexture("ColorMap", getApplication().getAssetManager().loadTexture("Textures/backboard.png"));

        boxGeometry.setMaterial(boxMat);

        RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        boxGeometry.addControl(rigidBodyControl);
        physicsSpace.addCollisionObject(rigidBodyControl);

        rootNodeDelegate.attachChild(boxGeometry);
    }

    /**
     * The players feet are at the height of the observer, but the x,z of the cameras
     * @return
     */
    private Vector3f getPlayerFeetPosition(){
        return vrAppState.getPlayerFeetPosition();
    }

    private Node getObserver(){
        return vrAppState.getObserver();
    }

    private Geometry debugBox(ColorRGBA colorRGBA){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", colorRGBA);
        boxGeometry.setMaterial(boxMat);
        return boxGeometry;
    }

}