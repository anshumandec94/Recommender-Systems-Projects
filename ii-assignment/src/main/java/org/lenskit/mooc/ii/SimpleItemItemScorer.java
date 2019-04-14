package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;


/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);

        // TODO Normalize the user's ratings by subtracting the item mean from each one.
        for(long itemIDs : ratings.keySet())
        {
            double normalized = ratings.get(itemIDs) - itemMeans.get(itemIDs);
            ratings.put(itemIDs, normalized);

        }

        List<Result> results = new ArrayList<>();
        double numerator=0;
        double denominator = 0;
        double ui_score =0;
        for (long item: items ) {
            numerator=0;
            denominator=0;
            // TODO Compute the user's score for each item, add it to results
            Long2DoubleMap itemNeighbors = model.getNeighbors(item);
            LinkedHashMap<Long,Double> sortedNeighbors = sortMap(itemNeighbors);
            int count =0;
            for(Map.Entry<Long,Double> neighbor : sortedNeighbors.entrySet())
            {
                if(count<neighborhoodSize) {

                    if (ratings.containsKey(neighbor.getKey())) {

                        numerator += ratings.get(neighbor.getKey()) * neighbor.getValue();
                        denominator += Math.abs(neighbor.getValue());
                        count++;

                    }
                    else
                        continue;
                }
                else
                    break;


            }
            ui_score = itemMeans.get(item)+(numerator/denominator);
            results.add(Results.create(item, ui_score));
        }

        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }
    private LinkedHashMap<Long,Double> sortMap (Long2DoubleMap unsorted)
    {
        List<Map.Entry<Long,Double>> list =  new LinkedList<Map.Entry<Long,Double>>(unsorted.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Long, Double>>() {
            @Override
            public int compare(Map.Entry<Long, Double> o1, Map.Entry<Long, Double> o2) {
                if(o1.getValue()-o2.getValue()>0)
                    return 1;
                else if(o1.getValue()-o2.getValue()<0)
                    return -1;
                else
                    return 0;

            }
        });
        Collections.reverse(list);
        LinkedHashMap<Long, Double> resultList=  new LinkedHashMap<Long,Double>();
        for(Map.Entry<Long,Double> e: list)
        {
            resultList.put(e.getKey(),e.getValue());
        }
        return resultList;


    }


}

