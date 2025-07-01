package cs1302.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a result in a response from the BallDon'tLie API.
 * Contains player's id, stats, and team. Some variables serialized
 * so that they will pass checkstyle.
 */
public class PlayerResponse {
    @SerializedName("first_name") String firstName;
    @SerializedName("last_name") String lastName;
    int id;
    double pts;
    double reb;
    double ast;
    double stl;
    double blk;
    double turnover;
    @SerializedName("fg_pct") double fgPct;
    @SerializedName("fg3_pct") double fg3Pct;
    @SerializedName("ft_pct") double ftPct;
    Team team;
    @SerializedName("full_name") String fullName;
}
