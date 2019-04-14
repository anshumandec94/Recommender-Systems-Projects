package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemBasedItemScorer;
import org.lenskit.results.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Global item scorer to find similar items.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */

public class SimpleItemBasedItemScorer extends AbstractItemBasedItemScorer {
    private final SimpleItemItemModel model;
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelProvider.class);

    @Inject
    public SimpleItemBasedItemScorer(SimpleItemItemModel mod) {
        model = mod;
    }

    /**
     * Score items with respect to a set of reference items.
     * @param basket The reference items.
     * @param items The score vector. Its domain is the items to be scored, and the scores should
     *               be stored into this vector.
     */
    @Override
    public ResultMap scoreRelatedItemsWithDetails(@Nonnull Collection<Long> basket, Collection<Long> items) {
        List<Result> results = new ArrayList<>();
        for (long item : items)
        {
            Long2DoubleMap itemVector = model.getNeighbors(item);
            double item_score = 0;
            for(long bItem : basket)
            {
                item_score +=itemVector.get(bItem);



            }
           // logger.debug("similarity "+itemVector.get(260));
           // logger.debug("Item " + item + " score " + item_score);
            results.add(Results.create(item,item_score));
        }
        // TODO Score the items and put them in results

        return Results.newResultMap(results);
    }
}
