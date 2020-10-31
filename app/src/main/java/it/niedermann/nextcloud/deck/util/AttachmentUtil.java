package it.niedermann.nextcloud.deck.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import it.niedermann.nextcloud.deck.DeckLog;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.model.Attachment;

/**
 * Created by stefan on 07.03.20.
 */

public class AttachmentUtil {

    private AttachmentUtil() {
    }

    /**
     * @return {@link AttachmentUtil#getRemoteUrl} or {@link Attachment#getLocalPath()} as fallback in case this {@param attachment} has not yet been synced.
     */
    @Nullable
    public static String getRemoteOrLocalUrl(@NonNull String accountUrl, @Nullable Long cardRemoteId, @NonNull Attachment attachment) {
        return (attachment.getId() == null || cardRemoteId == null)
                ? attachment.getLocalPath()
                : getRemoteUrl(accountUrl, cardRemoteId, attachment.getId());
    }

    /**
     * Tries to open the given {@link Attachment} in web browser. Displays a toast on failure.
     */
    public static void openAttachmentInBrowser(@NonNull Context context, @NonNull String accountUrl, Long cardRemoteId, Long attachmentRemoteId) {
        if (cardRemoteId == null) {
            Toast.makeText(context, R.string.card_does_not_yet_exist, Toast.LENGTH_LONG).show();
            DeckLog.logError(new IllegalArgumentException("cardRemoteId must not be null."));
            return;
        }
        if (attachmentRemoteId == null) {
            Toast.makeText(context, R.string.attachment_does_not_yet_exist, Toast.LENGTH_LONG).show();
            DeckLog.logError(new IllegalArgumentException("attachmentRemoteId must not be null."));
            return;
        }
        context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AttachmentUtil.getRemoteUrl(accountUrl, cardRemoteId, attachmentRemoteId))));
    }

    private static String getRemoteUrl(@NonNull String accountUrl, @NonNull Long cardRemoteId, @NonNull Long attachmentRemoteId) {
        return accountUrl + "/index.php/apps/deck/cards/" + cardRemoteId + "/attachment/" + attachmentRemoteId;
    }

    public static File copyContentUriToTempFile(@NonNull Context context, @NonNull Uri currentUri, long accountId, Long localCardId) throws IOException, IllegalArgumentException {
        final InputStream inputStream = context.getContentResolver().openInputStream(currentUri);
        if (inputStream == null) {
            throw new IOException("Could not open input stream for " + currentUri.getPath());
        }
        final File cacheFile = getTempCacheFile(context, accountId, localCardId, UriUtils.getDisplayNameForUri(currentUri, context));
        DeckLog.verbose("----- fullTempPath: " + cacheFile.getAbsolutePath());
        final File tempDir = cacheFile.getParentFile();
        if (tempDir == null) {
            throw new FileNotFoundException("could not cacheFile.getParentFile()");
        }
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                throw new IOException("Directory for temporary file does not exist and could not be created.");
            }
        }
        if (!cacheFile.createNewFile()) {
            throw new IOException("Failed to create cacheFile");
        }
        final FileOutputStream outputStream = new FileOutputStream(cacheFile);
        byte[] buffer = new byte[4096];

        int count;
        while ((count = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, count);
        }
        DeckLog.verbose("----- wrote");
        return cacheFile;
    }

    public static File getTempCacheFile(@NonNull Context context, long accountId, Long localCardId, String fileName) {
        return new File(context.getApplicationContext().getFilesDir().getAbsolutePath() + "/attachments/account-" + accountId + "/card-" + (localCardId == null ? "pending-creation" : localCardId) + '/' + fileName);
    }
}
