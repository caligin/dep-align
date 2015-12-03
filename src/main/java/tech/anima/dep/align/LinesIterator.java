package tech.anima.dep.align;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LinesIterator implements Iterator<String> {

    private final BufferedReader reader;
    private String prefetched;

    public LinesIterator(BufferedReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader must be not-null");
        }
        this.reader = reader;
        nextLine();
    }

    @Override
    public boolean hasNext() {
        return prefetched != null;
    }

    @Override
    public String next() {
        if (prefetched == null) {
            throw new NoSuchElementException("Iterator consumed");
        }
        final String next = prefetched;
        nextLine();
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Iterator is read-only");
    }

    private void nextLine() {
        try {
            this.prefetched = reader.readLine();
        } catch (IOException ex) {
            throw new IllegalStateException("IO error tokenizing string", ex);
        }
    }

}
