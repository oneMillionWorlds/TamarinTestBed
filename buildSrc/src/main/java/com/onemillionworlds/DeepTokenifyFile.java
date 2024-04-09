package com.onemillionworlds;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Geometry;
import com.onemillionworlds.deeptokens.DeepTokenBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This process an image to a deep token during the build process.
 */
public class DeepTokenifyFile extends DefaultTask{

    public File inputFile;

    public float tokenWidth;

    public float tokenDepth;

    public File outputFile;

    @TaskAction
    public void processFile() {
        DeepTokenBuilder builder = new DeepTokenBuilder(tokenWidth, tokenDepth);
        builder.setMinimumSharpAngle((float)Math.toRadians(60));
        AssetManager assetManager = new DesktopAssetManager(true);
        BinaryExporter exporter = BinaryExporter.getInstance();

        try {
            BufferedImage image = ImageIO.read(inputFile);
            Geometry result = builder.bufferedImageToLitGeometry(image, assetManager);
            exporter.save(result, outputFile);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            throw new GradleException("Error occurred During Deep tokenifying", e);
        }
    }

    @InputFile
    public File getInputDirectory(){
        return inputFile;
    }

    @OutputFile
    public File getOutDirectory(){
        return outputFile;
    }

    @Input
    public float getTokenWidth(){
        return tokenWidth;
    }

    @Input
    public float getTokenDepth(){
        return tokenDepth;
    }


    public void setInputFile(File inputFile){
        this.inputFile = inputFile;
    }

    public void setTokenDepth(float tokenDepth){
        this.tokenDepth = tokenDepth;
    }

    public void setTokenWidth(float tokenWidth){
        this.tokenWidth = tokenWidth;
    }

    public void setOutputDirectory(File outputFile){
        this.outputFile = outputFile;
    }

}
