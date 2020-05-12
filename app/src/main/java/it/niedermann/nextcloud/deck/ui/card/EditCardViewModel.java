package it.niedermann.nextcloud.deck.ui.card;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Card;
import it.niedermann.nextcloud.deck.model.full.FullCard;

@SuppressWarnings("WeakerAccess")
public class EditCardViewModel extends ViewModel {

    private Account account;
    private long boardId;
    private FullCard originalCard;
    private FullCard fullCard;
    private boolean isSupportedVersion = false;
    private boolean hasCommentsAbility = false;
    private boolean pendingCreation = false;
    private boolean canEdit = false;
    private boolean createMode = false;

    /**
     * Stores a deep copy of the given fullCard to be able to compare the state at every time in #{@link EditCardViewModel#hasChanges()}
     *
     * @param boardId  Local ID, expecting a positive long value
     * @param fullCard The card that is currently edited
     */
    public void initializeExistingCard(long boardId, @NonNull FullCard fullCard, boolean isSupportedVersion) {
        this.boardId = boardId;
        this.fullCard = fullCard;
        this.originalCard = new FullCard(this.fullCard);
        this.isSupportedVersion = isSupportedVersion;
    }

    /**
     * Stores a deep copy of the given fullCard to be able to compare the state at every time in #{@link EditCardViewModel#hasChanges()}
     *
     * @param boardId Local ID, expecting a positive long value
     * @param stackId Local ID, expecting a positive long value where the card should be created
     */
    public void initializeNewCard(long boardId, long stackId, boolean isSupportedVersion) {
        final FullCard fullCard = new FullCard();
        fullCard.setLabels(new ArrayList<>());
        fullCard.setAssignedUsers(new ArrayList<>());
        fullCard.setAttachments(new ArrayList<>());
        final Card card = new Card();
        card.setStackId(stackId);
        fullCard.setCard(card);
        initializeExistingCard(boardId, fullCard, isSupportedVersion);
    }

    public void setAccount(@NonNull Account account) {
        this.account = account;
        hasCommentsAbility = account.getServerDeckVersionAsObject().supportsComments();
    }

    public boolean hasChanges() {
        return fullCard.equals(originalCard);
    }

    public boolean hasCommentsAbility() {
        return hasCommentsAbility;
    }

    public Account getAccount() {
        return account;
    }

    public FullCard getFullCard() {
        return fullCard;
    }

    public boolean isPendingCreation() {
        return pendingCreation;
    }

    public void setPendingCreation(boolean pendingCreation) {
        this.pendingCreation = pendingCreation;
    }

    public boolean canEdit() {
        return canEdit && isSupportedVersion;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCreateMode() {
        return createMode;
    }

    public void setCreateMode(boolean createMode) {
        this.createMode = createMode;
    }

    public long getBoardId() {
        return boardId;
    }
}
