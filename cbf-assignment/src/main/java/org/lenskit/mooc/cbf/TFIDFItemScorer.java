package org.lenskit.mooc.cbf;

import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TFIDFItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final TFIDFModel model;
    private final UserProfileBuilder profileBuilder;
    private static final Logger logger = LoggerFactory.getLogger(TFIDFModelProvider.class);
    /**
     * Construct a new item scorer.  LensKit's dependency injector will call this constructor and
     * provide the appropriate parameters.
     *
     * @param dao The data access object, for looking up users' ratings.
     * @param m   The precomputed model containing the item tag vectors.
     * @param upb The user profile builder for building user tag profiles.
     */
    @Inject
    public TFIDFItemScorer(DataAccessObject dao, TFIDFModel m, UserProfileBuilder upb) {
        this.dao = dao;
        model = m;
        profileBuilder = upb;
    }

    /**
     * Generate item scores personalized for a particular user.  For the TFIDF scorer, this will
     * prepare a user profile and compare it to item tag vectors to produce the score.
     *
     * @param user   The user to score for.
     * @param items  A collection of item ids that should be scored.
     */
    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items){
        // Get the user's ratings
        List<Rating> ratings = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        if (ratings == null) {
            // the user doesn't exist, so return an empty ResultMap
            return Results.newResultMap();
        }

        // Create a place to store the results of our score computations
        List<Result> results = new ArrayList<>();

        // Get the user's profile, which is a vector with their 'like' for each tag
        Map<String, Double> userVector = profileBuilder.makeUserProfile(ratings);
        Set<String> userTags = userVector.keySet();
       // Set<String> commonTag = new Set<>();

        Double NormUser= new Double(0);
        for( String userIterator : userVector.keySet())// we calculate the normalized magnitude of the user's profile vector
        {
            NormUser += Math.pow(userVector.get(userIterator),2);
        }
        //  P

        for (Long item: items) {
            Map<String, Double> iv = model.getItemVector(item);

            // TODO Compute the cosine of this item and the user's profile, store it in the output list
            // TODO And remove this exception to say you've implemented it
            // If the denominator of the cosine similarity is 0, skip the item
            Double dotProduct= new Double(0);
            Double cosineValue =  new Double(0);
            Set<String> itemTags = iv.keySet();
            Double NormItem= new Double ( 0);
            for(String ItemTerator : itemTags)//we generate the normalized value of the item's term vector
            {
                NormItem+= Math.pow(iv.get(ItemTerator),2);
                //Q
            }

            for(String commonTags : itemTags)//we iterate through the items tags looking for the tags common with the user's profile tags
            {
                if(userTags.contains(commonTags))// that check is done here
                {
                    dotProduct+= userVector.get(commonTags)*iv.get(commonTags);//we calculate the doc product summation for the common tags here.

                }
            }
            if(NormUser*NormItem!=0)// if the normalized user value and normalized item vector is not zero then we can generate a cosine value.
                cosineValue = dotProduct/Math.sqrt(NormUser*NormItem);
            else                    // otherwise it's zero.
                cosineValue = 0.0;
            results.add(Results.create(item,cosineValue));//add the cosine value for that item to the results.


            //throw new UnsupportedOperationException("stub implementation");
        }

        return Results.newResultMap(results);
    }
}
































































