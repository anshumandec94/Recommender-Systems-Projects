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
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();
        //generatng user mean rating
        Double meanUserRating=0.0;
        for(Rating r: ratings)// we iterate over the user's ratings to get a sum of his/her ratings.
        {
            meanUserRating+=r.getValue();
        }
        meanUserRating=meanUserRating/ratings.size();//calculate the mean by dividing by the size of the ratings list.

        for(Rating r:ratings) //iterate through the ratings to get the individual items the user has rated.
        {


                Long itemID = r.getItemId();//get the item that has been rated
                Map<String,Double> currentItemVector = model.getItemVector(itemID);//get the item vector for the given item.
                for(String tag: currentItemVector.keySet())
                {
                    Double uservalue=0.0;
                    if(profile.get(tag)==null)// check if the tag has been initialized for the user or not
                    {
                         uservalue = (r.getValue()-meanUserRating)*currentItemVector.get(tag);//we get the user value for that tag by subtracting user rating form the mean rating and multiplying by the item's value for that tag.
                    }
                    else
                    {
                        uservalue = profile.get(tag);//same as above but get we add to the earlier value of user preference.
                        uservalue= uservalue+((r.getValue()-meanUserRating)*currentItemVector.get(tag));
                    }
                    profile.put(tag, uservalue);
                }

        }
        // TODO Normalize the user's ratings
        // TODO Build the user's weighted profile


        // The profile is accumulated, return it.
        return profile;
    }
}
