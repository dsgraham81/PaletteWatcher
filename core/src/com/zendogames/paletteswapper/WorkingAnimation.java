package com.zendogames.paletteswapper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Brian Ploeckelman created on 8/9/2014.
 *
 * TODO : address the use of static fields/methods in this class
 */
public class WorkingAnimation {

	static final String default_texture_filename = "eye.png";

	static final float frame_rate_min   = 0.015f;
	static final float frame_step_small = 0.00025f;
	static final float frame_step_big   = 0.0025f;
	static final float default_frame_rate = 0.15f;

	float thumbnail_width = 32;
	float thumbnail_height = 32;

	static Map<String, Texture> textures;
	static Animation animation;
	static float framerate;
	static float animTimer;

	Texture default_texture;
	TextureRegion keyframe;
	boolean paused;


	public WorkingAnimation() {
		initializeDefaultAnimation();
	}


	/**
	 * Update the animation state
	 *
	 * @param delta the time between this frame and the last
	 */
	public void update(float delta) {
		// Adjust frame rate in small steps
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			framerate += frame_step_small;
			refresh();
		}
		else if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			framerate -= frame_step_small;
			if (framerate <= frame_rate_min) framerate = frame_rate_min;
			refresh();
		}

		// Adjust frame rate in big steps
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			framerate += frame_step_big;
			refresh();
		}
		else if (Gdx.input.isKeyPressed(Keys.DOWN)) {
			framerate -= frame_step_big;
			if (framerate <= frame_rate_min) framerate = frame_rate_min;
			refresh();
		}

		// Update animation timer
		if (!paused) {
			animTimer += delta;
		}

		// Get current keyframe (assuming a valid animation)
		if (animation.getAnimationDuration() != 0) {
			keyframe = (TextureRegion) animation.getKeyFrame(animTimer);
		}
	}

	/**
	 * Render the animation user interface with the specified SpriteBatch
	 *
	 * @param batch the SpriteBatch to draw with
	 */
	public void renderUI(Batch batch) {
		final float initial_pos_x = 5;
		final float initial_pos_y = 35;
		final float keyframe_offset_height = 0;
		final float filename_text_offset_width = 10;
		final float window_height_offset = 100;

		final Set<Map.Entry<String, Texture>> entrySet = textures.entrySet();

		float x = initial_pos_x;
		float y = initial_pos_y;

		// Determine how many thumbnails fit on screen and which keyframe
		// should be drawn first so that the current keyframe always shows
		float window_height = Gdx.graphics.getHeight();
		float thumbnails_container_height = window_height - window_height_offset - initial_pos_y;
		int num_thumbnails_in_container = 0;
		int max_thumbnails_in_container = (int) (thumbnails_container_height / thumbnail_height);
		int starting_index = 0;
		int keyframe_index = animation.getKeyFrameIndex(animTimer);
		if (keyframe_index > max_thumbnails_in_container) {
			starting_index = keyframe_index - max_thumbnails_in_container;
		}

		for (int i = starting_index; i < animation.getKeyFrames().length; ++i) {
			TextureRegion kf = (TextureRegion) animation.getKeyFrames()[i];

			// Highlight the current keyframe thumbnail, darken the rest
			if (keyframe != kf) {
				batch.setColor(0.2f, 0.2f, 0.2f, 0.75f);
				AppState.font.setColor(0.2f, 0.2f, 0.2f, 0.75f);
			} else {
				batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
				AppState.font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
			}
			batch.draw(kf, x, y, thumbnail_width, thumbnail_height);

			// Find this keyframe's filename and draw it next to the thumbnail
			String keyframeFilename = "";
			for (Map.Entry<String, Texture> entry : entrySet) {
				if (entry.getValue() == kf.getTexture()) {
					keyframeFilename = entry.getKey().replace(".png", "");
					break;
				}
			}
			AppState.font.draw(batch, keyframeFilename,
					x + thumbnail_width + filename_text_offset_width,
					y + (thumbnail_height / 2f) + (AppState.font.getLineHeight() / 4f)
			);

			// Next row
			y += thumbnail_height + keyframe_offset_height;

			// Reset the color if necessary
			if (keyframe != kf) {
				batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
				AppState.font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
			}

			// Don't draw more than will fit on screen
			if (++num_thumbnails_in_container > max_thumbnails_in_container) {
				break;
			}
		}
	}

	/**
	 * Clear the current animation frames, reverting to the default animation
	 */
	public void clear() {
		textures.clear();
		animation = new Animation(1f, new TextureRegion(default_texture));
		animation.setPlayMode(PlayMode.LOOP);
	}

	/**
	 * Change the current play mode
	 */
	public void changePlayMode(PlayMode newPlayMode) {
		animation.setPlayMode(newPlayMode);
	}

	/**
	 * Refresh animation frames based on current watch path
	 */
	public static void refresh() {
		// Scan directory for pngs
		File watchPathFile = WorkingDirectory.watchPath.toFile();
		if (watchPathFile != null) {
			File[] files = WorkingDirectory.watchPath.toFile().listFiles();
			if (files == null) return;

			// Update filename->texture map from the current watch path
			for (File file : files) {
				if (file.isFile() && file.toString().endsWith(".png")) {
					String filename = file.getName();
					try {
						Texture texture = new Texture(Gdx.files.absolute(file.getAbsolutePath()));
						textures.put(filename, texture);
					} catch (Exception e) {
						// TODO : if '8-bit only' message, display to user
						Gdx.app.log("TEXTURE LOAD FAILURE", "Failed to load '" + filename + "', exception: " + e.getMessage());
					}
				}
			}

			// Sort filenames alphabetically
			String[] keys = textures.keySet().toArray(new String[textures.keySet().size()]);
			Arrays.sort(keys);

			// Get animation frames
			Array<TextureRegion> regions = new Array<TextureRegion>();
			for (String key : keys) {
				regions.add(new TextureRegion(textures.get(key)));
			}

			// Update the animation with the new frames (and possibly frame_rate)
			if (regions.size > 0) {
				animation = new Animation(framerate, regions);
				animation.setPlayMode(PlayMode.LOOP);
			}
		}
	}

	/**
	 * Handle a 'filesystem modified' event by updating the animation
	 *
	 * @param filename the name of the modified file
	 * @param filepath the path of the modified file
	 */
	public static void modify(Path filename, Path filepath) {
		if (!filename.toString().endsWith(".png")) {
			return;
		}
		Gdx.app.log("MODIFY EVENT", "received modify event for " + filepath.toString());

		try {
			FileHandle fileHandle = Gdx.files.absolute(filepath.toString());
			textures.put(filename.toString(), new Texture(fileHandle));
		} catch (Exception e) {
			// TODO : if '8-bit only' message, display to user
			Gdx.app.log("TEXTURE LOAD FAILURE", "Failed to load '" + filename.toString() + "', exception: " + e.getMessage());
		}

		String[] keys = textures.keySet().toArray(new String[textures.keySet().size()]);
		Arrays.sort(keys);

		Array<TextureRegion> regions = new Array<TextureRegion>();
		for (String key : keys) {
			regions.add(new TextureRegion(textures.get(key)));
		}
		animation = new Animation(framerate, regions);
		animation.setPlayMode(PlayMode.LOOP);
		animTimer = 0;
	}

	/**
	 * Load the default single frame animation
	 */
	private void initializeDefaultAnimation() {
		if (textures == null) {
			textures = new HashMap<String, Texture>();
		}

		if (default_texture == null) {
			default_texture = new Texture(Gdx.files.internal(default_texture_filename));
			default_texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}

		animation = new Animation(1, new TextureRegion(default_texture));
		animation.setPlayMode(PlayMode.LOOP);

		keyframe = (TextureRegion) animation.getKeyFrame(0);

		framerate = default_frame_rate;
		animTimer = 0;
		paused = false;
	}

}
