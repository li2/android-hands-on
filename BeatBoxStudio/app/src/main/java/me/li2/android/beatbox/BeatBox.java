package me.li2.android.beatbox;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by weiyi on 15/12/30.
 */
public class BeatBox {
    private static final String TAG = "BeatBox";
    private static final String SOUNDS_FOLDER = "sample_sounds";
    private static final int MAX_SOUNDS = 5;

    private AssetManager mAssetManager;
    private List<Sound> mSounds = new ArrayList<Sound>();
    private SoundPool mSoundPool;

    public BeatBox(Context context) {
        mAssetManager = context.getAssets();
        mSoundPool = buildSoundPool();
        loadSounds();
    }

    public void play(Sound sound) {
        Integer soundId = sound.getSoundId();
        if (soundId == null) {
            return;
        }
        // play the sound specified by the soundId
        mSoundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void release() {
        // release all memory and native resources used by the SoundPool object.
        mSoundPool.release();
    }

    private void loadSounds() {
        String[] soundNames;
        try {
            soundNames = mAssetManager.list(SOUNDS_FOLDER);
            Log.i(TAG, "found " + soundNames.length + " sounds");
        } catch (IOException ioe) {
            Log.e(TAG, "Could not list assets", ioe);
            return;
        }

        for (String fileName : soundNames) {
            try {
                String assetPath = SOUNDS_FOLDER + "/" + fileName;
                Sound sound = new Sound(assetPath);
                load(sound);
                mSounds.add(sound);
            } catch (IOException ioe) {
                Log.e(TAG, "Could not load sound " + fileName, ioe);
            }
        }
    }

    public List<Sound> getSounds() {
        return mSounds;
    }

    private void load(Sound sound) throws IOException {
        AssetFileDescriptor afd = mAssetManager.openFd(sound.getAssetPath());
        // load the sound from an asset file descriptor, and
        // return the soundId which can be used to play or unload the sound.
        int soundId = mSoundPool.load(afd, 1);
        sound.setSoundId(soundId);
    }

    // SoundPool manages and plays audio resources for applications.
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private SoundPool buildSoundPool() {
        SoundPool soundPool;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .setMaxStreams(MAX_SOUNDS)
                    .build();
        } else {
            // The constructor was deprecated in API level 21 (Android 5.0 LOLLIPOP).
            soundPool = new SoundPool(MAX_SOUNDS, AudioManager.STREAM_MUSIC, 0);
        }
        return soundPool;
    }
}
