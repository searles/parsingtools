package at.searles.parsing.tools.list;

import at.searles.parsing.Fold;
import at.searles.parsing.ParserStream;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fold to append elements to a list.
 *
 * @param <T>
 */
public class Append<T> implements Fold<List<T>, T, List<T>> {

    private final int minSize; // for inversion. If false, left may not be empty.

    public Append(int minSize) {
        this.minSize = minSize;
    }

    @Override
    public List<T> apply(ParserStream stream, @NotNull List<T> left, @NotNull T right) {
        return ImmutableList.createFrom(left).pushBack(right);
    }

    private boolean cannotInvert(List<T> list) {
        return list.size() <= minSize;
    }

    @Override
    public List<T> leftInverse(@NotNull List<T> result) {
        if (cannotInvert(result)) {
            return null;
        }

        return result.subList(0, result.size() - 1);
    }

    @Override
    public T rightInverse(@NotNull List<T> result) {
        if (cannotInvert(result)) {
            return null;
        }

        return result.get(result.size() - 1);
    }

    @Override
    public String toString() {
        return "{append}";
    }
}
