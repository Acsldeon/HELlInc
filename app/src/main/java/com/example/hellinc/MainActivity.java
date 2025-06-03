package com.example.hellinc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button startButton;
    private Button continueButton;
    private Button musicToggleButton;
    private Button exitButton;

    private static MediaPlayer mediaPlayer; // Static to persist across activities
    private boolean isMusicPlaying = false;
    private SharedPreferences sharedPreferences;

    public static final String PREFS_NAME = "HellInksPrefs";
    public static final String KEY_CURRENT_NODE = "currentNodeId";
    public static final String KEY_MUSIC_STATE = "isMusicPlaying";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        continueButton = findViewById(R.id.continueButton);
        musicToggleButton = findViewById(R.id.musicToggleButton);
        exitButton = findViewById(R.id.exitButton);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isMusicPlaying = sharedPreferences.getBoolean(KEY_MUSIC_STATE, false);

        setupMusicPlayer();
        updateMusicButtonText();
        checkContinueButtonVisibility();

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("startNewGame", true); // Флаг для начала новой игры
            startActivity(intent);
        });

        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("startNewGame", false); // Флаг для продолжения игры
            startActivity(intent);
        });

        musicToggleButton.setOnClickListener(v -> toggleMusic());

        exitButton.setOnClickListener(v -> finishAffinity()); // Закрывает все активити и выходит из приложения
    }

    private void checkContinueButtonVisibility() {
        String savedNode = sharedPreferences.getString(KEY_CURRENT_NODE, null);
        continueButton.setVisibility(savedNode != null ? View.VISIBLE : View.GONE);
    }

    private void setupMusicPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.menu_music);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true); // Зацикливание музыки
            }
        }
    }

    private void toggleMusic() {
        if (mediaPlayer != null) {
            if (isMusicPlaying) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
            isMusicPlaying = !isMusicPlaying;
            updateMusicButtonText();
            saveMusicState();
        }
    }

    private void updateMusicButtonText() {
        if (isMusicPlaying) {
            musicToggleButton.setText("Музыка: ВЫКЛ");
        } else {
            musicToggleButton.setText("Музыка: ВКЛ");
        }
    }

    private void saveMusicState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_MUSIC_STATE, isMusicPlaying);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMusicPlaying && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        checkContinueButtonVisibility(); // Проверяем видимость кнопки продолжения при возвращении в меню
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't release media player here as it's static and might be used by GameActivity
        // Release it when the application is truly shutting down or on a specific event
    }

    // Call this method when the app is completely done with the media player
    public static void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}