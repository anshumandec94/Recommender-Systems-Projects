package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item scorer that does a logistic blend of a subsidiary item scorer and popularity.  It tries to predict
 * whether a user has rated a particular item.
 */
public class LogisticItemScorer extends AbstractItemScorer {
    private final LogisticModel logisticModel;
    private final BiasModel biasModel;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;

    @Inject
    public LogisticItemScorer(LogisticModel model, UserBiasModel bias, RecommenderList recs, RatingSummary rs) {
        logisticModel = model;
        biasModel = bias;
        recommenders = recs;
        ratingSummary = rs;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {

        List<Result> results = new ArrayList<Result>();

        for(Long itemIterator : items)
        {

            double exp_var[] = new double[recommenders.getRecommenderCount()+2];
            double bias = biasModel.getIntercept()+biasModel.getUserBias(user)+biasModel.getItemBias(itemIterator);

            exp_var[0] =bias;
            exp_var[1] = Math.log(ratingSummary.getItemRatingCount(itemIterator));
            int i =2;
            for(ItemScorer itemS :recommenders.getItemScorers()) {
                Result r = itemS.score(user,itemIterator);
                if(r==null)
                    exp_var[i++] = 0 ;
                else
                    exp_var[i++]=r.getScore()-bias;


            }

            double prediction = logisticModel.evaluate(1,exp_var);// I'm assumed that we're predicting that a user would like to rate an item so I thought the specified output should be 1.
            results.add(Results.create(itemIterator,prediction));

        }
        return Results.newResultMap(results);

        // TODO Implement item scorer


    }
}
