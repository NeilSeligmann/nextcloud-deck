package it.niedermann.nextcloud.deck.ui;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import it.niedermann.nextcloud.deck.DeckLog;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.databinding.ActivityPushNotificationBinding;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.persistence.sync.SyncManager;
import it.niedermann.nextcloud.deck.ui.branding.Branded;
import it.niedermann.nextcloud.deck.ui.card.EditActivity;
import it.niedermann.nextcloud.deck.ui.exception.ExceptionHandler;

import static android.graphics.Color.parseColor;
import static it.niedermann.nextcloud.deck.persistence.sync.adapters.db.util.LiveDataHelper.observeOnce;
import static it.niedermann.nextcloud.deck.ui.branding.BrandedActivity.applyBrandToPrimaryToolbar;
import static it.niedermann.nextcloud.deck.ui.branding.BrandedActivity.applyBrandToStatusbar;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_ACCOUNT_ID;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_BOARD_ID;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_LOCAL_ID;

public class PushNotificationActivity extends AppCompatActivity implements Branded {

    private ActivityPushNotificationBinding binding;

    private boolean brandingEnabled;

    // Provided by Files app NotificationJob
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_LINK = "link";
    private static final String KEY_CARD_REMOTE_ID = "objectId";
    private static final String KEY_ACCOUNT = "account";

    @Override
    protected void onResume() {
        // when app is running in background or is starting after force reset
        super.onResume();

        Thread.currentThread().setUncaughtExceptionHandler(new ExceptionHandler(this));

        if (getIntent() == null) {
            throw new IllegalArgumentException("Could not retrieve intent");
        }

        binding = ActivityPushNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        brandingEnabled = getResources().getBoolean(R.bool.enable_brand);

        binding.subject.setText(getIntent().getStringExtra(KEY_SUBJECT));

        final String message = getIntent().getStringExtra(KEY_MESSAGE);
        if (!TextUtils.isEmpty(message)) {
            binding.message.setText(message);
            binding.message.setVisibility(View.VISIBLE);
        }

        final String link = getIntent().getStringExtra(KEY_LINK);

        binding.cancel.setOnClickListener((v) -> finish());

        final SyncManager syncManager = new SyncManager(this);
        final String cardRemoteIdString = getIntent().getStringExtra(KEY_CARD_REMOTE_ID);
        final String accountString = getIntent().getStringExtra(KEY_ACCOUNT);

        DeckLog.verbose("cardRemoteIdString = " + cardRemoteIdString);
        if (cardRemoteIdString != null) {
            try {
                final int cardRemoteId = Integer.parseInt(cardRemoteIdString);
                observeOnce(syncManager.readAccount(accountString), this, (account -> {
                    if (account != null) {
                        try {
                            if (brandingEnabled) {
                                applyBrand(parseColor(account.getColor()), parseColor(account.getTextColor()));
                            }
                        } catch (Throwable t) {
                            DeckLog.logError(t);
                        }
                        DeckLog.verbose("account: " + account);
                        observeOnce(syncManager.getLocalBoardIdByCardRemoteIdAndAccount(cardRemoteId, account), PushNotificationActivity.this, (boardLocalId -> {
                            DeckLog.verbose("BoardLocalId " + boardLocalId);
                            if (boardLocalId != null) {
                                observeOnce(syncManager.synchronizeCardByRemoteId(cardRemoteId, account), PushNotificationActivity.this, (fullCard -> {
                                    DeckLog.verbose("FullCard: " + fullCard);
                                    if (fullCard != null) {
                                        runOnUiThread(() -> {
                                            binding.submit.setOnClickListener((v) -> launchEditActivity(account.getId(), fullCard.getLocalId(), boardLocalId));
                                            binding.submit.setText(R.string.simple_open);
                                            applyBrandToSubmitButton(account);
                                            binding.submit.setEnabled(true);
                                            binding.progress.setVisibility(View.INVISIBLE);
                                        });
                                    } else {
                                        DeckLog.warn("Something went wrong while synchronizing the card " + cardRemoteId + " (cardRemoteId). Given fullCard is null.");
                                        applyBrandToSubmitButton(account);
                                        fallbackToBrowser(link);
                                    }
                                }));
                            } else {
                                DeckLog.warn("Given localBoardId for cardRemoteId " + cardRemoteId + " is null.");
                                applyBrandToSubmitButton(account);
                                fallbackToBrowser(link);
                            }
                        }));
                    } else {
                        DeckLog.warn("Given account for " + accountString + " is null.");
                        fallbackToBrowser(link);
                    }
                }));
            } catch (NumberFormatException e) {
                DeckLog.logError(e);
                fallbackToBrowser(link);
            }
        } else {
            DeckLog.warn(KEY_CARD_REMOTE_ID + " is null.");
            fallbackToBrowser(link);
        }
    }

    /**
     * If anything goes wrong and we cannot open the card directly, we fall back to open the given link in the webbrowser
     */
    private void fallbackToBrowser(String link) {
        DeckLog.warn("Falling back to browser as notification handler.");
        runOnUiThread(() -> {
            try {
                final Uri uri = Uri.parse(link);
                binding.submit.setOnClickListener((v) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(browserIntent);
                });
                binding.submit.setText(R.string.open_in_browser);
                binding.submit.setEnabled(true);
                binding.progress.setVisibility(View.INVISIBLE);
            } catch (Throwable t) {
                DeckLog.logError(t);
            }
        });
    }

    private void launchEditActivity(Long accountId, Long cardId, Long boardId) {
        DeckLog.info("starting activity with [" + accountId + ", " + cardId + ", " + boardId + "]");
        runOnUiThread(() -> {
            Intent intent = new Intent(this, EditActivity.class)
                    .putExtra(BUNDLE_KEY_ACCOUNT_ID, accountId)
                    .putExtra(BUNDLE_KEY_LOCAL_ID, cardId)
                    .putExtra(BUNDLE_KEY_BOARD_ID, boardId)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // close this activity as oppose to navigating up
        return true;
    }

    @Override
    public void applyBrand(@ColorInt int mainColor, @ColorInt int textColor) {
        if (brandingEnabled) {
            applyBrandToStatusbar(getWindow(), mainColor, textColor);
            applyBrandToPrimaryToolbar(mainColor, textColor, binding.toolbar);
        }
    }

    public void applyBrandToSubmitButton(@NonNull Account account) {
        try {
            binding.submit.setBackgroundColor(parseColor(account.getColor()));
            binding.submit.setTextColor(parseColor(account.getTextColor()));
        } catch (Throwable t) {
            DeckLog.logError(t);
        }
    }
}
