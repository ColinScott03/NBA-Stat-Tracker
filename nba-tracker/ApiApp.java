package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import java.text.DecimalFormat;

/**
 * The app that I created is an NBA Stat Finder app. With this app, a user can enter
 * the full name of an NBA player, as well as a year, past (dating back to 1980) or present,
 * and the app will query the BallDon'tLie API to retrieve data for the player's season
 * averages for the specified season, as well as the last team that this player played for.
 * Then, using the team information provided by the first query, the application queries the
 * SportsDB API to receive an image of the player's team logo and their arena.
 */
public class ApiApp extends Application {

    // setting up HTTP Client
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    // setting up GSON
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    Stage stage;
    Scene scene;
    VBox root;
    VBox images;
    HBox panel;
    HBox results;
    VBox stats;
    TextField searchField;
    ComboBox<String> year;
    Text perGame;
    Text teamDesc;
    Label loading;

    Text header;
    Button goButton;
    Button returnButton;
    String playerTeam;
    ImageView teamImage;
    ImageView arena;
    boolean isFound = true;
    String name;
    String last;

    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        root = new VBox();
        images = new VBox();
        header = new Text();
        goButton = new Button("Find Stats");
        panel = new HBox(5);
        results = new HBox(10);
        searchField = new TextField("LeBron James");
        year = new ComboBox<>();
        returnButton = new Button("Return Home");
        teamImage = new ImageView();
        stats = new VBox();
        perGame = new Text();
        arena = new ImageView();
        teamDesc = new Text();
        loading = new Label("Data provided by the BallDon'tLie API and the SportsDB Api.");
    } // ApiApp

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {

        this.stage = stage;
        stage.setMaxWidth(1280);
        stage.setMaxHeight(720);

        // background image for the home screen
        Image homeImage = new Image("file:resources/default.png");
        ImageView home = new ImageView(homeImage);
        home.setPreserveRatio(true);
        home.setFitWidth(640);

        // label to prompt the user
        Label notice = new Label("Enter an NBA player, select a year, and find their button!"
            + "\nPlease spell the desired player's full name to be the best of your ability.");

        // setup scene
        root.getChildren().addAll(notice, panel, results, home, loading);
        panel.getChildren().addAll(returnButton, searchField, year, goButton);
        scene = new Scene(root);
        // results are not visible from the home screen
        root.getChildren().get(2).setVisible(false);
        root.getChildren().get(2).setManaged(false);
        results.getChildren().addAll(stats, images);
        teamImage.setPreserveRatio(true);
        teamImage.setFitHeight(200);
        teamImage.setFitWidth(200);
        arena.setPreserveRatio(true);
        arena.setFitHeight(200);
        arena.setFitWidth(300);
        images.getChildren().addAll(teamDesc, teamImage, arena);
        stats.getChildren().addAll(header, perGame);
        header.setFont(Font.font("Arial", FontWeight.BOLD, 15)); // displays player's name
        images.setAlignment(Pos.CENTER);

        // setup stage
        stage.setTitle("NBA Stat Finder");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.sizeToScene();
        stage.show();

    } // start

    /** {@inheritDoc} */
    @Override
    public void init() {
        // adding valid years to dropdown
        for (int i = 2022; i >= 1980; i--) {
            String following = String.valueOf(i + 1).substring(2);
            year.getItems().add(String.valueOf(i) + "-" + following);
        }
        year.setValue("2022-23"); // default value for year
        HBox.setHgrow(searchField, Priority.ALWAYS);
        returnButton.setDisable(true);
        goButton.setOnAction(e -> {
            Thread t = new Thread(() -> { // new thread created when button is pressed
                Platform.runLater(() -> goButton.setDisable(true));
                if (this.retrieve() != -1) { // only gets stats if the player was found
                    this.getStats(this.retrieve());
                }
            });
            t.setDaemon(true);
            t.start();
        });
        //this.retrieve();

        // when home button is pressed, results becomes invisible and home image becomes visible
        returnButton.setOnAction(e -> {
            root.getChildren().get(3).setVisible(true);
            root.getChildren().get(3).setManaged(true);
            root.getChildren().get(2).setVisible(false);
            root.getChildren().get(2).setManaged(false);
            returnButton.setDisable(true);
            loading.setText("Data provided by the BallDon'tLie API and the SportsDB API.");
        });
    }

    /**
     * Queries the BallDon'tLie API to retrieve a player's specific id number and name.
     * Resets results as necessary.
     *
     * @return the id of the player, or -1 if player was not found
     */
    public int retrieve() {
        try {
            // resets results
            header.setText(null);
            perGame.setText(null);
            Platform.runLater(() -> loading.setText(
                "Loading...please be mindful of limited query calls"));
            teamImage.setImage(null);
            arena.setImage(null);
            teamDesc.setText(null);
            String player = searchField.getText(); // retrieves user input
            player = player.replace(" ", "%20"); // formats name for query
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create
                ("https://www.balldontlie.io/api/v1/players?search=" + player))
                .build();

            HttpResponse<String> response = HTTP_CLIENT
                .send(request, BodyHandlers.ofString());

            // throws exception if invalid statusCode
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            }

            String jsonString = response.body();

            BallDontLieResponse x = new Gson().fromJson(jsonString, BallDontLieResponse.class);

            // throws exception if player isn't found
            if (x.data.length == 0) {
                throw new IllegalArgumentException("Player could not be found.");
            }

            name = x.data[0].firstName;
            last = x.data[0].lastName;

            playerTeam = x.data[0].team.fullName; // name of player's team

            return x.data[0].id; // returns player's id

        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            this.queryCatch(e); // catches exception in helper method
            return -1; // returns -1 if player isn't found
        }


    }

    /**
     * Queries the BallDon'tLie API to retrieve a player's averages for a specified season.
     * Updates scene to show stats.
     *
     * @param id the id of the player to be searched
     */
    public void getStats(int id) {
        try {
            // method returns if player wasn't found
            if (isFound = false) {
                isFound = true;
                return;
            }
            String season = year.getValue(); // retrieves selected year
            season = season.substring(0, 4);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create
                ("https://www.balldontlie.io/api/v1/season_averages?"
                + "season=" + season + "&player_ids[]=" + id + "&postseason=false"))
                .build();

            HttpResponse<String> response = HTTP_CLIENT
                .send(request, BodyHandlers.ofString());

            // throws exception if statusCode is invalid
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            }

            String jsonString = response.body();

            BallDontLieResponse x = new Gson().fromJson(jsonString, BallDontLieResponse.class);

            // throws exception if no stats were found for the given year
            if (x.data.length == 0) {
                throw new IllegalArgumentException("This player did not record any stats during the"
                + " selected season.");
            }
            double pts = x.data[0].pts;
            double reb = x.data[0].reb;
            double ast = x.data[0].ast;
            double stl = x.data[0].stl;
            double blk = x.data[0].blk;
            double turnover = x.data[0].turnover;
            DecimalFormat df = new DecimalFormat("#.##"); // prevents very long decimal output
            String fgPct = df.format(x.data[0].fgPct * 100);
            String fg3Pct = df.format(x.data[0].fg3Pct * 100);
            String ftPct = df.format(x.data[0].ftPct * 100);

            // adequately formats output
            header.setText(name + " " + last + " " + year.getValue() + " averages:");
            perGame.setText("\nPoints per Game:\t\t" + pts + "\n\n\nRebounds per Game:\t" + reb +
                "\n\n\nAssists per Game:\t\t" + ast + "\n\n\nSteals per Game:\t\t" + stl +
                "\n\n\nBlocks per Game:\t\t" + blk + "\n\n\nTurnovers per Game:\t" +
                turnover + "\n\n\nField Goal Percentage:\t" + fgPct +
                "%\n\n\n3-Point Percentage:\t\t" + fg3Pct + "%\n\n\nFree Throw Percentage:\t" +
                ftPct + "%");
            this.getArena(); // calls upon other method to retrieve images if method was successful

        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            this.queryCatch(e); // catches exceptions in helper method
        }
    }

    /**
     * Queries the SportsDB API to receive the image of a team's logo and an image of their
     * arena based on the team named retrieved from the BallDon'tLie API query.
     */
    public void getArena() {
        try {
            teamDesc.setText("Loading team data..."); // loading message
            String teamName = playerTeam;
            teamName = teamName.replace(" ", "%20"); // format query String
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create
                ("https://www.thesportsdb.com/api/v1/json/60130162/searchteams.php?"
                + "t=" + teamName))
                .build();

            HttpResponse<String> response = HTTP_CLIENT
                .send(request, BodyHandlers.ofString());

            // throws exception if statusCode is invalid
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            }

            String jsonString = response.body();

            SportsDBResponse x = new Gson().fromJson(jsonString, SportsDBResponse.class);

            // image of the team's logo
            String logoURL = x.teams[0].strTeamBadge;
            Image logo = new Image(logoURL);

            // image of the team's arena
            String arenaURL = x.teams[0].strStadiumThumb;
            Image arenaPic = new Image(arenaURL);

            arena.setImage(arenaPic);
            teamImage.setImage(logo);
            teamDesc.setText("Last team played for:\n" + x.teams[0].strTeam);

            // updates scene if all methods were successful
            Platform.runLater(() -> {
                loading.setText("Done.");
                root.getChildren().get(2).setVisible(true);
                root.getChildren().get(2).setManaged(true);
                root.getChildren().get(3).setVisible(false);
                root.getChildren().get(3).setManaged(false);
                goButton.setDisable(false);
                returnButton.setDisable(false);
            });

        } catch (IOException | InterruptedException e) {
            this.queryCatch(e); // catches exceptions in helper method
        }
    }

    /**
     * Displays error messages and returns to home screen if exceptions are thrown in previous
     * methods.
     *
     * @param e An exception thrown by a previous method in which this method is called.
     */
    public void queryCatch(Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getClass().getName() + ": " + e.getMessage());
            alert.showAndWait();

            // returns to home screen
            root.getChildren().get(3).setVisible(true);
            root.getChildren().get(3).setManaged(true);
            root.getChildren().get(2).setVisible(false);
            root.getChildren().get(2).setManaged(false);
            returnButton.setDisable(true);
            goButton.setDisable(false);
            loading.setText("Data provided by the BallDon'tLie API and the SportsDB API.");
        });
    }
} // ApiApp
