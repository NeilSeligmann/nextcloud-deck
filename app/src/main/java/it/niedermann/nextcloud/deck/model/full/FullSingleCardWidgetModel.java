package it.niedermann.nextcloud.deck.model.full;

import androidx.room.Embedded;
import androidx.room.Relation;

import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.widget.singlecard.SingleCardWidgetModel;

public class FullSingleCardWidgetModel {

    @Embedded
    private SingleCardWidgetModel model;

    @Relation(parentColumn = "localId", entityColumn = "accountId")
    private Account account;

    @Relation(parentColumn = "localId", entityColumn = "cardId")
    private FullCard fullCard;

    public SingleCardWidgetModel getModel() {
        return model;
    }

    public void setModel(SingleCardWidgetModel model) {
        this.model = model;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public FullCard getFullCard() {
        return fullCard;
    }

    public void setFullCard(FullCard fullCard) {
        this.fullCard = fullCard;
    }
}
