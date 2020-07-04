package it.niedermann.nextcloud.deck.model.ocs.user;

public class OcsUser {
    String id;
    String displayName;

    public OcsUser() {

    }

    public OcsUser(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OcsUser ocsUser = (OcsUser) o;

        if (id != null ? !id.equals(ocsUser.id) : ocsUser.id != null) return false;
        return displayName != null ? displayName.equals(ocsUser.displayName) : ocsUser.displayName == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        return result;
    }
}
