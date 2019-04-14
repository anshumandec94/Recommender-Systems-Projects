package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class ThresholdUserProfileBuilder implements UserProfileBuilder {
    /**
     * The lowest rating that will be considered in the user's profile.
     */
    private static final double RATING_THRESHOLD = 3.5;

    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public ThresholdUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();

        // Iterate over the user's ratings to build their profile
        for (Rating r: ratings) {
            if (r.getValue() >= RATING_THRESHOLD) {


                // TODO Get this item's vector and add it to the user's profile

                Long Item= r.getItemId();//we get the item ID for the item that is greater than the user's threshold.
               Map<String,Double> currentItemVector = model.getItemVector(Item);
               for(String tag:currentItemVector.keySet())
               {
                   Double uservalue=(profile.get(tag)!=null)?profile.get(tag):0.0;// if the profile doesn't have the tag then initialize it, otherwise get the earlier value.
                   uservalue =uservalue+currentItemVector.get(tag);// increment the user preference for the tag by adding the item's score for that tag
                   profile.put(tag,uservalue);//put it into the user profile


               }


            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}
