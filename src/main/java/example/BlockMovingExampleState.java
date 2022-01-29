package example;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.onemillionworlds.tamarin.vrhands.grabbing.AutoMovingGrabControl;

public class BlockMovingExampleState extends BaseAppState{

    Node rootNodeDelegate = new Node("MenuExampleState");

    @Override
    protected void initialize(Application app){
        ((SimpleApplication)app).getRootNode().attachChild(rootNodeDelegate);
        initialiseScene();
    }

    @Override
    protected void cleanup(Application app){
        rootNodeDelegate.removeFromParent();
    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }

    private void initialiseScene(){
        Quad floorQuad = new Quad(10,10);
        Geometry floor = new Geometry("floor", floorQuad);
        Texture floorTexture = getApplication().getAssetManager().loadTexture("Textures/checkerBoard.png");
        floorTexture.setMagFilter(Texture.MagFilter.Nearest);
        Material mat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", floorTexture);

        floor.setMaterial(mat);
        Quaternion floorRotate = new Quaternion();
        floorRotate.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        floor.setLocalRotation(floorRotate);
        floor.setLocalTranslation(-5,0,15);
        rootNodeDelegate.attachChild(floor);

        grabbableBox(new Vector3f(0,1f, 9.5f));
        grabbableBox(new Vector3f(0.2f,1.2f, 9.5f));
        grabbableBox(new Vector3f(-0.2f,0.9f, 9.5f));
        grabbableBox(new Vector3f(0.3f,1.1f, 9.6f));
    }

    private void grabbableBox(Vector3f location){
        Box box = new Box(0.05f, 0.05f, 0.05f);
        Geometry boxGeometry = new Geometry("box", box);
        Material boxMat = new Material(getApplication().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.randomColor());
        boxGeometry.setMaterial(boxMat);
        boxGeometry.setLocalTranslation(location);
        AutoMovingGrabControl grabControl = new AutoMovingGrabControl(new Vector3f(0.025f,0,0), 0.05f);
        boxGeometry.addControl(grabControl);
        rootNodeDelegate.attachChild(boxGeometry);
    }
}
