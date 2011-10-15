package cgeo.geocaching.utils;

public abstract class RunnableWithArgument<T> implements Runnable {

    private T argument;

    public void setArgument(final T argument) {
        this.argument = argument;
    }

    public T getArgument() {
        return argument;
    }
}
