package com.onemillionworlds;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Geometry;
import com.onemillionworlds.deeptokens.DeepTokenBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This takes layers on images and using transparency, overlays them on top of each other.
 * <p>
 * All images should be the same size.
 * </p>
 */
public class DeepTokenifyDirectory extends DefaultTask{

    public File inputDirectory;

    public float tokenWidth;

    public float tokenDepth;

    public File outputDirectory;

    @TaskAction
    public void processFile() {
        DeepTokenBuilder builder = new DeepTokenBuilder(tokenWidth, tokenDepth);
        AssetManager assetManager = new DesktopAssetManager(true);
        BinaryExporter exporter = BinaryExporter.getInstance();

        for( File fileToProcess : inputDirectory.listFiles()){
            if (fileToProcess.getName().endsWith(".png")){
                try {

                    BufferedImage image = ImageIO.read(fileToProcess);

                    Geometry result = builder.bufferedImageToLitGeometry(image, assetManager);

                    int vertextes = result.getMesh().getVertexCount();

                    System.out.print("Processing " + fileToProcess.getName() + " with " + vertextes + " vertextes");

                    File destination = new File(outputDirectory, fileToProcess.getName().replace(".png", ".j3o"));
                    exporter.save(result, destination);
                } catch (IOException e) {
                    throw new GradleException("Error occurred During Deep tokenifying", e);
                }
            }
        }
    }

    @InputDirectory
    public File getInputDirectory(){
        return inputDirectory;
    }

    @OutputDirectory
    public File getOutDirectory(){
        return outputDirectory;
    }

    @Input
    public float getTokenWidth(){
        return tokenWidth;
    }

    @Input
    public float getTokenDepth(){
        return tokenDepth;
    }

    public void setInputDirectory(File directory){
        this.inputDirectory = directory;
    }

    public void setTokenDepth(float tokenDepth){
        this.tokenDepth = tokenDepth;
    }

    public void setTokenWidth(float tokenWidth){
        this.tokenWidth = tokenWidth;
    }

    public void setOutputDirectory(File outputDirectory){
        this.outputDirectory = outputDirectory;
    }

}
