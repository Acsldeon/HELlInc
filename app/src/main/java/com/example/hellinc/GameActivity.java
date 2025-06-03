package com.example.hellinc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Import for AlertDialog
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;

// GameActivity больше не реализует SettingsDialogListener, так как DialogFragment убран
public class GameActivity extends AppCompatActivity {

    private TextView storyTextView;
    private ImageView imageView;
    private LinearLayout choicesContainer;
    private ImageButton settingsButton;
    private ScrollView textScrollView;

    private String currentNodeId;
    private SharedPreferences sharedPreferences;

    private MediaPlayer mediaPlayer;
    private boolean isMusicPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        storyTextView = findViewById(R.id.storyTextView);
        imageView = findViewById(R.id.imageView);
        choicesContainer = findViewById(R.id.choicesContainer);
        settingsButton = findViewById(R.id.settingsButton);
        textScrollView = findViewById(R.id.textScrollView);

        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        isMusicPlaying = sharedPreferences.getBoolean(MainActivity.KEY_MUSIC_STATE, false);

        // Get the static MediaPlayer instance from MainActivity
        try {
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("mediaPlayer");
            field.setAccessible(true);
            mediaPlayer = (MediaPlayer) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e("GameActivity", "Could not get static MediaPlayer field: " + e.getMessage());
        }

        boolean startNewGame = getIntent().getBooleanExtra("startNewGame", false);

        if (savedInstanceState != null) {
            currentNodeId = savedInstanceState.getString(MainActivity.KEY_CURRENT_NODE);
        } else if (!startNewGame) {currentNodeId = sharedPreferences.getString(MainActivity.KEY_CURRENT_NODE, StoryData.START_NODE_ID);
        } else {
            currentNodeId = StoryData.START_NODE_ID;
            // Очищаем сохраненное состояние, если начинается новая игра
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(MainActivity.KEY_CURRENT_NODE);
            editor.apply();
        }

        displayNode(currentNodeId);

        settingsButton.setOnClickListener(v -> showSettingsDialog());
    }

    private void toggleMusic() {
        if (mediaPlayer != null) {
            if (isMusicPlaying) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
            isMusicPlaying = !isMusicPlaying;
            saveMusicState();
        }
    }

    private void saveMusicState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainActivity.KEY_MUSIC_STATE, isMusicPlaying);
        editor.apply();
    }

    // Изменен метод showSettingsDialog для использования AlertDialog
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.settings_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Находим кнопки внутри кастомного layout
        Button saveGameButton = dialogView.findViewById(R.id.saveGameButton);
        Button musicToggleButton = dialogView.findViewById(R.id.musicToggleButtonSettings);
        Button continueGameButton = dialogView.findViewById(R.id.continueGameButton);
        Button exitToMainMenuButton = dialogView.findViewById(R.id.exitToMainMenuButton);

        // Устанавливаем слушатели
        saveGameButton.setOnClickListener(v -> {
            onSaveGame(); // Вызываем внутренний метод сохранения
            dialog.dismiss();
        });

        musicToggleButton.setOnClickListener(v -> {
            onToggleMusic(); // Вызываем внутренний метод переключения музыки
            // Обновляем текст кнопки на диалоге, если это необходимо
            if (isMusicPlaying) {
                musicToggleButton.setText("Музыка: ВЫКЛ");
            } else {
                musicToggleButton.setText("Музыка: ВКЛ");
            }
        });

        continueGameButton.setOnClickListener(v -> dialog.dismiss()); // Просто закрыть диалог

        exitToMainMenuButton.setOnClickListener(v -> {
            onExitToMainMenu(); // Вызываем внутренний метод выхода в меню
            dialog.dismiss();
        });

        dialog.show();
    }


    private void displayNode(String nodeId) {
        StoryData.StoryNode node = StoryData.getStoryNodes().get(nodeId);
        if (node == null) {
            Log.e("GameActivity", "Node not found: " + nodeId);
            storyTextView.setText(R.string.game_over);
            choicesContainer.removeAllViews();
            // Optionally add a button to return to main menu at the end
            Button backToMenuButton = new Button(this);
            backToMenuButton.setText(R.string.back_to_main_menu);
            backToMenuButton.setOnClickListener(v -> onExitToMainMenu());
            choicesContainer.addView(backToMenuButton);
            choicesContainer.setVisibility(View.VISIBLE);
            return;
        }

        currentNodeId = nodeId;
        storyTextView.setText(node.getText());
        textScrollView.scrollTo(0,0); // Прокручиваем текст в начало

        if (node.getImageResId() != 0) {
            imageView.setImageResource(node.getImageResId());
        } else {
            imageView.setImageResource(R.drawable.starviev); // Дефолтное изображение
        }

        choicesContainer.removeAllViews(); // Очистить предыдущие кнопки

        if (node.getChoices() != null && !node.getChoices().isEmpty()) {
            for (StoryData.Choice choice : node.getChoices()) {
                Button choiceButton = new Button(this, null, 0, R.style.ChoiceButton);
                choiceButton.setText(choice.getText());

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, // Width 0 for weight to work
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.weight = 1; // Distribute space evenly
                params.setMarginEnd(8); // Margin between buttons
                params.setMarginStart(8);
                choiceButton.setLayoutParams(params);

                choiceButton.setOnClickListener(v -> displayNode(choice.getNextNodeId()));
                choicesContainer.addView(choiceButton);
            }
            choicesContainer.setVisibility(View.VISIBLE);
        } else {
            choicesContainer.setVisibility(View.GONE); // Скрыть контейнер, если нет выбора

            // If it's a specific ending, add a "Back to Main Menu" button
            // This is especially important for end nodes that don't transition elsewhere
            Button backToMenuButton = new Button(this);
            backToMenuButton.setText(R.string.back_to_main_menu);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = View.TEXT_ALIGNMENT_CENTER; // Center the button
            backToMenuButton.setLayoutParams(params);
            backToMenuButton.setOnClickListener(v -> onExitToMainMenu());
            choicesContainer.addView(backToMenuButton);
            choicesContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MainActivity.KEY_CURRENT_NODE, currentNodeId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure music state is consistent with MainActivity
        isMusicPlaying = sharedPreferences.getBoolean(MainActivity.KEY_MUSIC_STATE, false);
        if (isMusicPlaying && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
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
        // Don't release MediaPlayer here as it's static and managed by MainActivity
    }

    // Методы, ранее бывшие частью интерфейса SettingsDialogListener, теперь являются внутренними
    public void onSaveGame() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MainActivity.KEY_CURRENT_NODE, currentNodeId);
        editor.apply();
    }

    public void onToggleMusic() {
        toggleMusic();
    }

    public void onExitToMainMenu() {
        Intent intent = new Intent(GameActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Очистить стек активити
        startActivity(intent);
        finish(); // Закрыть GameActivity
    }
}