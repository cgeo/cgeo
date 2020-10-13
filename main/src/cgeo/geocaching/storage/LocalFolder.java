package cgeo.geocaching.storage;

public enum LocalFolder {

    LOGFILES("logfiles"),
    BACKUP("backup"),
    OFFLINE_MAP("maps");

    private final String folderName;

    LocalFolder(final String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDefaultMimeType() {
        //TODO make configurable
        return "text/plain";
    }

}
