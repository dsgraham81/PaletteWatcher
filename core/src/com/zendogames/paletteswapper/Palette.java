package com.zendogames.paletteswapper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Palette {

    static final String default_palette = "palette.png";

    public static File textureFile;
    PaletteWatcher watcher;
    public static Texture paletteTexture;

    public int paletteIndex;
    public static TextureRegion paletteRowTexRegion;


    public Palette(){
//        setFile(default_palette);
    }

    public void setFile(String file) {
        textureFile = new File(file);
        try {
            Path texturePath = Paths.get(textureFile.getAbsolutePath());
            watcher = new PaletteWatcher(texturePath.getParent());
            watcher.processEvents();

        } catch (IOException e) {
            e.printStackTrace();
        }
        updateTexture();
    }

    public void updatePaletteFile() {
        String cwd = null;
        if (textureFile != null){
             cwd = textureFile.getAbsolutePath();
        }
        final JFileChooser fileChooser = new JFileChooser(cwd);
        try {
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    fileChooser.showOpenDialog(null);
                }
            });
        } catch(Exception e) {
            Gdx.app.log("FILE CHOOSER", "File chooser error: " + e.getMessage());
        }

        final File chosenFile = fileChooser.getSelectedFile();
        if (chosenFile != null) {
            Gdx.app.log("FILE CHOOSER", "Directory: " + chosenFile.getAbsolutePath());
            setFile(chosenFile.getAbsolutePath());
        }
    }

    public static void updateTexture() {

        paletteTexture = new Texture(Gdx.files.absolute(textureFile.getAbsolutePath()));
        paletteRowTexRegion = new TextureRegion(paletteTexture);
    }

    public void renderRow(SpriteBatch batch, OrthographicCamera camera) {
        if (paletteTexture == null) return;
        batch.setColor(Color.WHITE);
        paletteRowTexRegion.setRegion(0f, paletteIndex/255f, 1f, (paletteIndex+1)/255f);
        float y = camera.viewportHeight - 105;
        float width = camera.viewportWidth - 205;
        batch.draw(paletteRowTexRegion, 0, y, width, 5);
    }
}
