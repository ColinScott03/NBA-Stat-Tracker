package cs1302.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a Team object from the result of the BallDon'tLie API response.
 * Contains the name of the player's team. Variable serialized to pass checkstyle.
 */
public class Team {
    @SerializedName("full_name") String fullName;
}
