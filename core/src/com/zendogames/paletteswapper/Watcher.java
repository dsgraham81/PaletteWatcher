package com.zendogames.paletteswapper;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Watcher extends ApplicationAdapter {
	SpriteBatch batch;
	AppState appState;
	ShaderProgram paletteSwapShader;

	@Override
	public void create () {
		batch = new SpriteBatch();
		appState = new AppState();
		paletteSwapShader = compileShaderProgram(
				Gdx.files.internal("shaders/default.vert"),
				Gdx.files.internal("shaders/palette-swap.frag"));


		InputMultiplexer mux = new InputMultiplexer();
		mux.addProcessor(new InputHandler(appState));
		mux.addProcessor(appState.ui.stage);
		Gdx.input.setInputProcessor(mux);
	}

	@Override
	public void resize(int width, int height) {
		appState.resize(width, height);
	}


	@Override
	public void render () {
		appState.update(Gdx.graphics.getDeltaTime());

		Color bg = appState.ui.background;
		Gdx.gl.glClearColor(bg.r, bg.g, bg.b, bg.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		sceneRender();
		batch.setProjectionMatrix(appState.cameras.hudCamera.combined);
		batch.begin();
		appState.palette.renderRow(batch, appState.cameras.hudCamera);
		batch.end();
		appState.ui.render();
	}

	private void sceneRender() {
		float whw = Gdx.graphics.getWidth() / 2f;
		float whh = Gdx.graphics.getHeight() / 2f;
		float thw = appState.workingAnimation.keyframe.getRegionWidth() / 2f;
		float thh = appState.workingAnimation.keyframe.getRegionHeight() / 2f;

		int vw = (int) appState.cameras.sceneCamera.viewportWidth;
		int vh = (int) appState.cameras.sceneCamera.viewportHeight;
		Gdx.gl20.glViewport(0, 0, vw, vh);

		batch.setProjectionMatrix(appState.cameras.sceneCamera.combined);
		batch.begin();

		batch.setShader(paletteSwapShader);
		appState.palette.paletteTexture.bind(1);
		paletteSwapShader.setUniformi("u_palette", 1);
		appState.workingAnimation.keyframe.getTexture().bind(0);
		paletteSwapShader.setUniformi("u_texture", 0);

		batch.setColor(appState.palette.paletteIndex /255f, 1, 1, 1);
		batch.draw(appState.workingAnimation.keyframe, whw - thw, whh - thh);

		batch.setShader(null);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}

	private static ShaderProgram compileShaderProgram(FileHandle vertSource, FileHandle fragSource) {
		ShaderProgram.pedantic = false;
		final ShaderProgram shader = new ShaderProgram(vertSource, fragSource);
		if (!shader.isCompiled()) {
			throw new GdxRuntimeException("Failed to compile shader program:\n" + shader.getLog());
		}
		else if (shader.getLog().length() > 0) {
			Gdx.app.debug("SHADER", "ShaderProgram compilation log:\n" + shader.getLog());
		}
		return shader;
	}
}
