package it.niedermann.nextcloud.deck.persistence.sync.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.api.ApiProvider;
import it.niedermann.nextcloud.deck.api.IResponseCallback;
import it.niedermann.nextcloud.deck.api.LastSyncUtil;
import it.niedermann.nextcloud.deck.api.RequestHelper;
import it.niedermann.nextcloud.deck.exceptions.OfflineException;
import it.niedermann.nextcloud.deck.model.AccessControl;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Card;
import it.niedermann.nextcloud.deck.model.Label;
import it.niedermann.nextcloud.deck.model.Stack;
import it.niedermann.nextcloud.deck.model.full.FullBoard;
import it.niedermann.nextcloud.deck.model.full.FullCard;
import it.niedermann.nextcloud.deck.model.full.FullStack;
import it.niedermann.nextcloud.deck.model.ocs.Capabilities;
import it.niedermann.nextcloud.deck.model.propagation.CardUpdate;
import it.niedermann.nextcloud.deck.model.propagation.Reorder;
import it.niedermann.nextcloud.deck.util.DateUtil;

public class ServerAdapter {

    String prefKeyWifiOnly;

    private static final DateFormat API_FORMAT =
            new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss z", Locale.US);

    static {
        API_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private Context applicationContext;
    private ApiProvider provider;
    @Nullable private Activity sourceActivity;
    private SharedPreferences lastSyncPref;

    public ServerAdapter(Context applicationContext, @Nullable Activity sourceActivity) {
        this.applicationContext = applicationContext;
        this.sourceActivity = sourceActivity;
        prefKeyWifiOnly = applicationContext.getResources().getString(R.string.pref_key_wifi_only);
        provider = new ApiProvider(applicationContext);
        lastSyncPref = applicationContext.getSharedPreferences(
                applicationContext.getString(R.string.shared_preference_last_sync), Context.MODE_PRIVATE);
    }

    public String getServerUrl() throws NextcloudFilesAppAccountNotFoundException, NoCurrentAccountSelectedException {
        return provider.getServerUrl();
    }

    public String getApiPath() {
        return provider.getApiPath();
    }

    public String getApiUrl() throws NextcloudFilesAppAccountNotFoundException, NoCurrentAccountSelectedException {
        return provider.getApiUrl();
    }

    public void ensureInternetConnection() {
        boolean isConnected = hasInternetConnection();
        if (!isConnected){
            throw new OfflineException();
        }
    }

    public boolean hasInternetConnection(){
        ConnectivityManager cm = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            if (sharedPreferences.getBoolean(prefKeyWifiOnly, false)){
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Network network = cm.getActiveNetwork();
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    if (capabilities == null) {
                        return false;
                    }
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                } else {
                    NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (networkInfo == null) {
                        return false;
                    }
                    return networkInfo.isConnected();
                }


            } else {
                return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
            }
        }
        return false;
    }

    private String getLastSyncDateFormatted(long accountId) {
        return null;
//        String lastSyncHeader = API_FORMAT.format(getLastSync(accountId));
//        // omit Offset of timezone (e.g.: +01:00)
//        if (lastSyncHeader.matches("^.*\\+[0-9]{2}:[0-9]{2}$")) {
//            lastSyncHeader = lastSyncHeader.substring(0, lastSyncHeader.length()-6);
//        }
//        DeckLog.log("lastSync "+lastSyncHeader);
//        return lastSyncHeader;
    }

    private Date getLastSync(long accountId) {
        Date lastSync = DateUtil.nowInGMT();
        lastSync.setTime(LastSyncUtil.getLastSync(accountId));

        return lastSync;
    }

    public void getBoards(IResponseCallback<List<FullBoard>> responseCallback) {
        RequestHelper.request(sourceActivity, provider, () ->
                provider.getDeckAPI().getBoards(true, getLastSyncDateFormatted(responseCallback.getAccount().getId())),
                responseCallback);
    }
    public void getCapabilities(IResponseCallback<Capabilities> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getNextcloudAPI().getCapabilities(), responseCallback);
    }

    public void getActivitiesForCard(long cardId, IResponseCallback<List<it.niedermann.nextcloud.deck.model.ocs.Activity>> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getNextcloudAPI().getActivitiesForCard(cardId), responseCallback);
    }

    public void createBoard(Board board, IResponseCallback<FullBoard> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().createBoard(board), responseCallback);
    }


    public void deleteBoard(Board board, IResponseCallback<Void> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().deleteBoard(board.getId()), responseCallback);
    }

    public void updateBoard(Board board, IResponseCallback<FullBoard> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().updateBoard(board.getId(), board), responseCallback);
    }

    public void createAccessControl(long remoteBoardId, AccessControl acl, IResponseCallback<AccessControl> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().createAccessControl(remoteBoardId, acl), responseCallback);
    }

    public void updateAccessControl(long remoteBoardId, AccessControl acl, IResponseCallback<AccessControl> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().updateAccessControl(remoteBoardId, acl.getId(), acl), responseCallback);
    }

    public void deleteAccessControl(long remoteBoardId, AccessControl acl, IResponseCallback<Void> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().deleteAccessControl(remoteBoardId, acl.getId(), acl), responseCallback);
    }

    public void getStacks(long boardId, IResponseCallback<List<FullStack>> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().getStacks(boardId, getLastSyncDateFormatted(responseCallback.getAccount().getId())), responseCallback);
    }

    public void getStack(long boardId, long stackId, IResponseCallback<FullStack> responseCallback) {
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().getStack(boardId, stackId, getLastSyncDateFormatted(responseCallback.getAccount().getId())), responseCallback);
    }

    public void createStack(Board board, Stack stack, IResponseCallback<FullStack> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().createStack(board.getId(), stack), responseCallback);
    }

    public void deleteStack(Board board, Stack stack, IResponseCallback<Void> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().deleteStack(board.getId(), stack.getId()), responseCallback);

    }

    public void updateStack(Board board, Stack stack, IResponseCallback<FullStack> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().updateStack(board.getId(), stack.getId(), stack), responseCallback);

    }

    public void getCard(long boardId, long stackId, long cardId, IResponseCallback<FullCard> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().getCard(boardId, stackId, cardId, getLastSyncDateFormatted(responseCallback.getAccount().getId())), responseCallback);
    }

    public void createCard(long boardId, long stackId, Card card, IResponseCallback<FullCard> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().createCard(boardId, stackId, card), responseCallback);
    }

    public void deleteCard(long boardId, long stackId, Card card, IResponseCallback<Void> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().deleteCard(boardId, stackId, card.getId()), responseCallback);
    }

    public void updateCard(long boardId, long stackId, CardUpdate card, IResponseCallback<FullCard> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().updateCard(boardId, stackId, card.getId(), card), responseCallback);
    }

    public void assignUserToCard(long boardId, long stackId, long cardId, String userUID, IResponseCallback<Void> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().assignUserToCard(boardId, stackId, cardId, userUID), responseCallback);
    }

    public void unassignUserFromCard(long boardId, long stackId, long cardId, String userUID, IResponseCallback<Void> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().unassignUserFromCard(boardId, stackId, cardId, userUID), responseCallback);
    }

    public void assignLabelToCard(long boardId, long stackId, long cardId, long labelId, IResponseCallback<Void> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().assignLabelToCard(boardId, stackId, cardId, labelId), responseCallback);
    }

    public void unassignLabelFromCard(long boardId, long stackId, long cardId, long labelId, IResponseCallback<Void> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().unassignLabelFromCard(boardId, stackId, cardId, labelId), responseCallback);
    }


    // ## LABELS
    public void createLabel(long boardId, Label label, IResponseCallback<Label> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().createLabel(boardId, label), responseCallback);
    }
    public void deleteLabel(long boardId, Label label, IResponseCallback<Void> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().deleteLabel(boardId, label.getId()), responseCallback);
    }
    public void updateLabel(long boardId, Label label, IResponseCallback<Label> responseCallback){
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().updateLabel(boardId, label.getId(), label), responseCallback);
    }

    public void reorder(Long boardId, FullCard movedCard, long newStackId, int newPosition, IResponseCallback<List<FullCard>> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().moveCard(boardId, movedCard.getCard().getStackId(), movedCard.getCard().getId(), new Reorder(newPosition, (int)newStackId)), responseCallback);
    }

    // ## ATTACHMENTS
    public void uploadAttachment(Long remoteBoardId, long remoteStackId, long remoteCardId, File attachment, IResponseCallback<Void> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().uploadAttachment(remoteBoardId, remoteStackId, remoteCardId, attachment), responseCallback);
    }
    public void deleteAttachment(Long remoteBoardId, long remoteStackId, long remoteCardId, long remoteAttachmentId, IResponseCallback<Void> responseCallback) {
        ensureInternetConnection();
        RequestHelper.request(sourceActivity, provider, () -> provider.getDeckAPI().deleteAttachment(remoteBoardId, remoteStackId, remoteCardId, remoteAttachmentId), responseCallback);
    }
}
