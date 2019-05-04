package it.niedermann.nextcloud.deck.ui.board;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.flexbox.FlexboxLayout;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.ui.MainActivity;
import it.niedermann.nextcloud.deck.util.ViewUtil;

public class BoardCreateDialogFragment extends DialogFragment {

    private Context context;
    private String selectedColor;
    private String previouslySelectedColor;
    private ImageView previouslySelectedImageView;

    @BindView(R.id.input)
    EditText boardTitle;
    @BindView(R.id.colorPicker)
    FlexboxLayout colorPicker;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_board_create, null);
        this.context = Objects.requireNonNull(getContext());
        ButterKnife.bind(this, view);

        initColorChooser();

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_board)
                .setView(view)
                .setNegativeButton(R.string.simple_cancel, (dialog, which) -> {
                    // Do something else
                })
                .setPositiveButton(R.string.simple_create, (dialog, which) -> {
                    ((MainActivity) getActivity()).onCreateBoard(boardTitle.getText().toString(), selectedColor);
                })
                .create();
    }

    private void initColorChooser() {
        String[] colors = getResources().getStringArray(R.array.board_default_colors);

        // TODO refactor color chooser as own View Component
        for (final String color : colors) {
            ImageView image = new ImageView(getContext());
            image.setOnClickListener((imageView) -> {
                if (previouslySelectedImageView != null) { // null when first selection
                    previouslySelectedImageView.setImageDrawable(ViewUtil.getTintedImageView(this.context, R.drawable.circle_grey600_36dp, previouslySelectedColor));
                }
                image.setImageDrawable(ViewUtil.getTintedImageView(this.context, R.drawable.circle_alpha_check_36dp, color));
                selectedColor = color;
                this.previouslySelectedColor = color;
                this.previouslySelectedImageView = image;
            });
            image.setImageDrawable(ViewUtil.getTintedImageView(this.context, R.drawable.circle_grey600_36dp, color));
            colorPicker.addView(image);
        }
    }
}