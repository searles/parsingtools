package at.searles.parsing.tools.map;

import at.searles.parsing.Initializer;
import at.searles.parsing.ParserStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by searles on 31.03.19.
 */
public class EmptyMap<K, V> implements Initializer<Map<K, V>> {

    @Override
    public Map<K, V> parse(ParserStream stream) {
        return new HashMap<>();
    }

    @Override
    public boolean consume(Map<K, V> map) {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return "{emptymap}";
    }
}
