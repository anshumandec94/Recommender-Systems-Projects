package org.lenskit.mooc.hybrid;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.lenskit.LenskitRecommender;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.inject.Transient;
import org.lenskit.util.ProgressLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

/**
 * Trainer that builds logistic models.
 */
public class LogisticModelProvider implements Provider<LogisticModel> {
    private static final Logger logger = LoggerFactory.getLogger(LogisticModelProvider.class);
    private static final double LEARNING_RATE = 0.00005;
    private static final int ITERATION_COUNT = 100;

    private final LogisticTrainingSplit dataSplit;
    private final BiasModel baseline;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;
    private final int parameterCount;
    private final Random random;

    @Inject
    public LogisticModelProvider(@Transient LogisticTrainingSplit split,
                                 @Transient UserBiasModel bias,
                                 @Transient RecommenderList recs,
                                 @Transient RatingSummary rs,
                                 @Transient Random rng) {
        dataSplit = split;
        baseline = bias;
        recommenders = recs;
        ratingSummary = rs;
        parameterCount = 1 + recommenders.getRecommenderCount() + 1;
        random = rng;
    }

    @Override
    public LogisticModel get() {
        List<ItemScorer> scorers = recommenders.getItemScorers();
        double intercept = 0;
        double[] params = new double[parameterCount];
        List<Rating> train_ratings =dataSplit.getTuneRatings();

        Map<Rating, RealVector> expVariables = new HashMap<>();
        LogisticModel current = LogisticModel.create(intercept, params);

        //List<ItemScorer> recs_item_scorers = recommenders.getItemScorers();
        for(Rating r : train_ratings) {//generate the explanatory variables array here.
            long item = r.getItemId();
            long user = r.getUserId();
            RealVector individual_scores=new ArrayRealVector(parameterCount);//array of length parameter count(since the number of exp variables
                                                                             // and parameters are the same.
            double bl = baseline.getIntercept()+baseline.getItemBias(item)+ baseline.getUserBias(user);//baseline term
            individual_scores.setEntry(0,bl);//add it as explanatory varaiable
            individual_scores.setEntry(1, Math.log(ratingSummary.getItemRatingCount(item)));//get the log of rating popularity
            int count = 2;
            for(ItemScorer i:recommenders.getItemScorers()) {
                Result score = i.score(user,item);
                logger.debug("user "+user+"item "+item+" score "+ score);
                double result;
                if(score ==null) {
                    result = 0;
                }
                else
                 result = score.getScore()-bl;
                logger.debug(" score"+ result);
                individual_scores.setEntry(count++,result);

            }
            //long key = (user*1000000000)+item;
            expVariables.put(r,individual_scores);


        }

        for(int i=0;i<ITERATION_COUNT;i++) {// training the dataset.
            //current.create(intercept, params);
            logger.info("Iteration " + i );
            Collections.shuffle(train_ratings);
            current = LogisticModel.create(intercept,params);

            for(Rating r : train_ratings) {
                long user= r.getUserId();
                long item = r.getItemId();
                //long key =(user*1000000000)+item;
                RealVector vars = expVariables.get(r);//getting the explanatory variables for the item we have.
                if(vars == null)// this is in the case of a situation where the cache of stored outputs doesn't contain the vector for this rating.
                {

                    vars=new ArrayRealVector(parameterCount);//array of length parameter count(since the number of exp variables
                    // and parameters are the same.
                    double bl = baseline.getIntercept()+baseline.getItemBias(item)+ baseline.getUserBias(user);//baseline term
                    vars.setEntry(0,bl);//add it as explanatory varaiable
                    vars.setEntry(1, Math.log(ratingSummary.getItemRatingCount(item)));//get the log of rating popularity
                    int count = 2;
                    for(ItemScorer is:recommenders.getItemScorers()) {
                        Result score = is.score(user,item);
                        logger.debug("user "+user+"item "+item+" score "+ score);
                        double result;
                        if(score ==null) {
                            result = 0;
                        }
                        else
                            result = score.getScore()-bl;
                        logger.debug(" score"+ result);
                        vars.setEntry(count++,result);

                    }
                }
                double update = current.evaluate(-r.getValue(), vars);


                intercept = intercept + (LEARNING_RATE*r.getValue()*update);
                for(int j = 0;j<parameterCount;j++) {
                    params[j] = params[j] + (LEARNING_RATE*vars.getEntry(j)*r.getValue()*update);// similar update for the parameters, multiplying the specific explanatory variable to the
                }
                current = LogisticModel.create(intercept,params);

            }

        }

        // TODO Implement model training

        return current;
    }

}
