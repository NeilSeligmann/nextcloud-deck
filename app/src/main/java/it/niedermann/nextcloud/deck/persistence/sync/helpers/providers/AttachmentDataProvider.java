package it.niedermann.nextcloud.deck.persistence.sync.helpers.providers;

import android.annotation.SuppressLint;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import it.niedermann.nextcloud.deck.DeckLog;
import it.niedermann.nextcloud.deck.api.ResponseCallback;
import it.niedermann.nextcloud.deck.model.Attachment;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Stack;
import it.niedermann.nextcloud.deck.model.full.FullCard;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.ServerAdapter;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.db.DataBaseAdapter;

public class AttachmentDataProvider extends AbstractSyncDataProvider<Attachment> {

    private FullCard card;
    private Board board;
    private Stack stack;
    private List<Attachment> attachments;

    public AttachmentDataProvider(AbstractSyncDataProvider<?> parent, Board board, Stack stack, FullCard card, List<Attachment> attachments) {
        super(parent);
        this.board = board;
        this.stack = stack;
        this.card = card;
        this.attachments = attachments;
    }

    @Override
    public Disposable getAllFromServer(ServerAdapter serverAdapter, long accountId, ResponseCallback<List<Attachment>> responder, Instant lastSync) {
        responder.onResponse(attachments);
        return new CompositeDisposable();
    }

    @Override
    public Attachment getSingleFromDB(DataBaseAdapter dataBaseAdapter, long accountId, Attachment entity) {
        return dataBaseAdapter.getAttachmentByRemoteIdDirectly(accountId, entity.getId());
    }

    @Override
    public long createInDB(DataBaseAdapter dataBaseAdapter, long accountId, Attachment attachment) {
        attachment.setCardId(card.getLocalId());
        return dataBaseAdapter.createAttachment(accountId, attachment);
    }

    @Override
    public void updateInDB(DataBaseAdapter dataBaseAdapter, long accountId, Attachment attachment, boolean setStatus) {
        attachment.setCardId(card.getLocalId());
        dataBaseAdapter.updateAttachment(accountId, attachment, setStatus);
    }

    @Override
    public void updateInDB(DataBaseAdapter dataBaseAdapter, long accountId, Attachment entity) {
        updateInDB(dataBaseAdapter, accountId, entity, false);
    }


    @Override
    public void deleteInDB(DataBaseAdapter dataBaseAdapter, long accountId, Attachment attachment) {
        dataBaseAdapter.deleteAttachment(accountId, attachment, false);
    }

    @Override
    public Disposable createOnServer(ServerAdapter serverAdapter, DataBaseAdapter dataBaseAdapter, long accountId, ResponseCallback<Attachment> responder, Attachment entity) {
        File file = new File(entity.getLocalPath());
        return serverAdapter.uploadAttachment(board.getId(), stack.getId(), card.getId(), file, new ResponseCallback<Attachment>(responder.getAccount()) {
            @Override
            public void onResponse(Attachment response) {
                if (file.delete()) {
                    responder.onResponse(response);
                } else {
                    responder.onError(new IOException("Could not delete local file after successful upload: " + file.getAbsolutePath()));
                }
            }

            @SuppressLint("MissingSuperCall")
            @Override
            public void onError(Throwable throwable) {
                if (!file.delete()) {
                    DeckLog.error("Could not delete local file:", file.getAbsolutePath());
                }
                // if (HandledServerErrors.ATTACHMENTS_FILE_ALREADY_EXISTS == HandledServerErrors.fromThrowable(throwable)) {
                dataBaseAdapter.deleteAttachment(accountId, entity, false);
                // }
                responder.onError(throwable);
            }
        });
    }

    @Override
    public Disposable updateOnServer(ServerAdapter serverAdapter, DataBaseAdapter dataBaseAdapter, long accountId, ResponseCallback<Attachment> callback, Attachment entity) {
        Uri uri = Uri.fromFile(new File(entity.getLocalPath()));
        String type = dataBaseAdapter.getContext().getContentResolver().getType(uri);
        return serverAdapter.updateAttachment(board.getId(), stack.getId(), card.getId(), entity.getId(), type, uri, callback);

    }

    @Override
    public Disposable deleteOnServer(ServerAdapter serverAdapter, long accountId, ResponseCallback<Void> callback, Attachment entity, DataBaseAdapter dataBaseAdapter) {
        return serverAdapter.deleteAttachment(board.getId(), stack.getId(), card.getId(), entity.getId(), callback);
    }

    @Override
    public List<Attachment> getAllChangedFromDB(DataBaseAdapter dataBaseAdapter, long accountId, Instant lastSync) {
        return dataBaseAdapter.getLocallyChangedAttachmentsByLocalCardIdDirectly(accountId, card.getLocalId());
    }

    @Override
    public void handleDeletes(ServerAdapter serverAdapter, DataBaseAdapter dataBaseAdapter, long accountId, List<Attachment> entitiesFromServer) {
        List<Attachment> localAttachments = dataBaseAdapter.getAttachmentsForLocalCardIdDirectly(accountId, card.getLocalId());
        List<Attachment> delta = findDelta(entitiesFromServer, localAttachments);
        for (Attachment attachment : delta) {
            if (attachment.getId() == null) {
                // not pushed up yet so:
                continue;
            }
            dataBaseAdapter.deleteAttachment(accountId, attachment, false);
        }
        for (Attachment attachment : entitiesFromServer) {
            if (attachment.getDeletedAt() != null && attachment.getDeletedAt().toEpochMilli() != 0) {
                Attachment toDelete = dataBaseAdapter.getAttachmentByRemoteIdDirectly(accountId, attachment.getId());
                if (toDelete != null) {
                    dataBaseAdapter.deleteAttachment(accountId, toDelete, false);
                }
            }
        }
    }
}
