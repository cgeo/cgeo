package cgeo.geocaching.wizard;

public enum WizardMode {
    WIZARDMODE_DEFAULT(0),
    WIZARDMODE_RETURNING(1),
    WIZARDMODE_MIGRATION(2);

    public final int id;

    WizardMode(final int id) {
        this.id = id;
    }
}
