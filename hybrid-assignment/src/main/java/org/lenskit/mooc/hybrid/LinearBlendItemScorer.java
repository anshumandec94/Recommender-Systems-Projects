package org.lenskit.mooc.hybrid;

import com.google.common.base.Preconditions;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item scorer that computes a linear blend of two scorers' scores.
 *
 * <p>This scorer takes two underlying scorers and blends their scores.
 */
public class LinearBlendItemScorer extends AbstractItemScorer {
    private final BiasModel biasModel;
    private final ItemScorer leftScorer, rightScorer;
    private final double blendWeight;

    /**
     * Construct a popularity-blending item scorer.
     *
     * @param bias The baseline bias model to use.
     * @param left The first item scorer to use.
     * @param right The second item scorer to use.
     * @param weight The weight to give popularity when ranking.
     */
    @Inject
    public LinearBlendItemScorer(BiasModel bias,
                                 @Left ItemScorer left,
                                 @Right ItemScorer right,
                                 @BlendWeight double weight) {
        Preconditions.checkArgument(weight >= 0 && weight <= 1, "weight out of range");
        biasModel = bias;
        leftScorer = left;
        rightScorer = right;
        blendWeight = weight;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        List<Result> results = new ArrayList<>();
        for(Long itemIterator : items)
        {
            double left_score,right_score;

                double bias_term = biasModel.getIntercept()+biasModel.getUserBias(user)+ biasModel.getItemBias(itemIterator);


                if(  leftScorer.score(user, itemIterator) !=null) {
                    Result left= leftScorer.score(user, itemIterator);
                   double left_value = left.getScore();
                   left_score = left_value - bias_term;

                }
                else{
                    left_score = 0.0;
                }


                if(rightScorer.score(user,itemIterator)!=null) {
                    Result right = rightScorer.score(user,itemIterator);
                    double right_value = right.getScore();
                    right_score = right_value - bias_term;
                }
                else {
                    right_score =0.0;
                }
                double weighted_score = bias_term + ((1-blendWeight)*(left_score))+((blendWeight)*(right_score));

                results.add(Results.create(itemIterator,weighted_score));

        }
        // TODO Compute hybrid scores

        return Results.newResultMap(results);
    }
}
