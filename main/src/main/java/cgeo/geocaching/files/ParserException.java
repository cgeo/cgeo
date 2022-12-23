package cgeo.geocaching.files;

/**
 * Exception indicating that a FileParser (GPX or LOC) could not parse the file due to bad file format.
 */
public class ParserException extends Exception {
    private static final long serialVersionUID = 1L;

    public ParserException() {
    }

    public ParserException(final String detailMessage) {
        super(detailMessage);
    }

    public ParserException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);
    }

}
