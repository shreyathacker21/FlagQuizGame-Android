package com.example.student.flagquizgame;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.IOException; //for reading image files from assets folder
import java.io.InputStream; //for reading image files from assets folder
import java.util.ArrayList; //for holding image file names and current quiz items
import java.util.Collections; //for shuffle method
import java.util.HashMap; //storing region names and corresponding Boolean values indicating whether each region is enabled or disabled.
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager; //for accessing files in assets folder
import android.graphics.drawable.Drawable; //for displaying the image file in an ImageView after reading it in.
import android.os.Handler; //used to execute a Runnable object in the future
import android.util.Log;  //used for logging exceptions for debugging purposes – viewed by using the Android logcat tool and are also displayed in the Android DDMS (Dalvik Debug Monitor Server) perspective’s LogCat tab in Eclipse.
import android.view.LayoutInflater;
import android.view.MenuItem; //along with Menu class used to display a context menu.
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;  //used for the animations
import android.view.animation.AnimationUtils;  //used for the animations
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FlagQuizGame extends AppCompatActivity {

    // String used when logging error messages
    private static final String TAG = "FlagQuizGame Activity";

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // names of countries in quiz
    private Map<String, Boolean> regionsMap; // which regions are enabled
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // number of rows displaying choices
    private Random random; // random number generator
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess

    private TextView answerTextView; // displays Correct! or Incorrect!
    private TextView questionNumberTextView; // shows current question #
    private ImageView flagImageView; // displays a flag
    private TableLayout buttonTableLayout; // table of answer Buttons

    // create constants for each menu id
    private final int CHOICES_MENU_ID = Menu.FIRST;
    private final int REGIONS_MENU_ID = Menu.FIRST + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flag_quiz_game);
        fileNameList = new ArrayList<String>(); // list of image file names
        quizCountriesList = new ArrayList<String>(); //flgas in this quiz
        regionsMap = new HashMap<String, Boolean>(); //HashMap of regions
        guessRows = 1; //default to 1 row choices
        random = new Random();
        handler =  new Handler(); // used to perform delayed operations
        shakeAnimation = AnimationUtils.loadAnimation(this,R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); //repeat animation 3 times

        //dynamically get array of world regions from strings.xml
        String[] regionNames = getResources().getStringArray(R.array.regionsList);

        //by default ,contries are chosen from all regions
        for(String region : regionNames){
            regionsMap.put(region,true);

        }

        //get references to GUI items
        questionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView)findViewById(R.id.flagImageView);
        buttonTableLayout = (TableLayout)findViewById(R.id.buttonTableLayout);
        answerTextView = (TextView)findViewById(R.id.answerTextView);


        //set the questionNumberTextView's text

        questionNumberTextView.setText(getResources().getString(R.string.question) +
                " 1 " +getResources().getString(R.string.of) + "10");
        resetQuiz(); //start a new quiz


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       //add 2 options to the menu
        //1st arguement menu item;s group id
        //2nd arguement is items unique id
        //3rd arguement is the order it appears
        //4th arguement is what is displayed
        menu.add(Menu.NONE,CHOICES_MENU_ID,Menu.NONE, R.string.choices);
        menu.add(Menu.NONE,REGIONS_MENU_ID,Menu.NONE,R.string.regions);


        return true;
    }
    // called when the user selects an option from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // switch the menu id of the user-selected option
        switch (item.getItemId())  //gets unique ID of the item
        {
            case CHOICES_MENU_ID:
                // create a list of the possible numbers of answer choices
                final String[] possibleChoices =
                        getResources().getStringArray(R.array.guessesList);

                // create a new AlertDialog Builder and set its title
                AlertDialog.Builder choicesBuilder =
                        new AlertDialog.Builder(this);
                choicesBuilder.setTitle(R.string.choices);

                // add possibleChoices's items to the Dialog and set the
                // behavior when one of the items is clicked
                choicesBuilder.setItems(R.array.guessesList,
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int item)
                            {
                                // update guessRows to match the user's choice
                                guessRows = Integer.parseInt(
                                        possibleChoices[item].toString()) / 3;
                                resetQuiz(); // reset the quiz
                            } // end method onClick
                        } // end anonymous inner class
                );  // end call to setItems

                // create an AlertDialog from the Builder
                AlertDialog choicesDialog = choicesBuilder.create();
                choicesDialog.show(); // show the Dialog
                return true;

            case REGIONS_MENU_ID:
                // get array of world regions
                final String[] regionNames =
                        regionsMap.keySet().toArray(new String[regionsMap.size()]);

                // boolean array representing whether each region is enabled
                boolean[] regionsEnabled = new boolean[regionsMap.size()];
                for (int i = 0; i < regionsEnabled.length; ++i)
                    regionsEnabled[i] = regionsMap.get(regionNames[i]);

                // create an AlertDialog Builder and set the dialog's title
                AlertDialog.Builder regionsBuilder =
                        new AlertDialog.Builder(this);
                regionsBuilder.setTitle(R.string.regions);

                // replace _ with space in region names for display purposes
                String[] displayNames = new String[regionNames.length];
                for (int i = 0; i < regionNames.length; ++i)
                    displayNames[i] = regionNames[i].replace('_', ' ');

                // add displayNames to the Dialog and set the behavior
                // when one of the items is clicked
                regionsBuilder.setMultiChoiceItems(
                        displayNames, regionsEnabled,
                        new DialogInterface.OnMultiChoiceClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked)
                            {
                                // include or exclude the clicked region
                                // depending on whether or not it's checked
                                regionsMap.put(
                                        regionNames[which].toString(), isChecked);
                            } // end method onClick
                        } // end anonymous inner class
                ); // end call to setMultiChoiceItems

                // resets quiz when user presses the "Reset Quiz" Button
                regionsBuilder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int button)
                            {
                                resetQuiz(); // reset the quiz
                            } // end method onClick
                        } // end anonymous inner class
                ); // end call to method setPositiveButton

                // create a dialog from the Builder
                AlertDialog regionsDialog = regionsBuilder.create();
                regionsDialog.show(); // display the Dialog
                return true;
        } // end switch

        return super.onOptionsItemSelected(item);
    } // end method onOptionsItemSelected



    private void resetQuiz() {
        //use the assest manager to get the image flag for enabled regions
        AssetManager assets = getAssets(); //get the app's assestmanager
        fileNameList.clear();
        try {
            Set<String> regions = regionsMap.keySet(); //get set of regions
            //loop through each region
            for (String region : regions) {
                if (regionsMap.get(region)) {
                    //if region is enabled , get a list of all flags for this regions
                    region = region.replace(' ', '_');
                    String[] paths = assets.list(region);
                    for (String path : paths) {
                        fileNameList.add(path.replace(".png", ""));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);

        }
        correctAnswers = 0; //reset
        totalGuesses = 0; //reset
        quizCountriesList.clear(); //clear prior questions

        //add 10 random file names to the list
        int flagCounter = 1;
        int numberOfFlags = fileNameList.size(); // number of flags

        while (flagCounter <= 10) {

            int randomIndex = random.nextInt(numberOfFlags);
            String fileName = fileNameList.get(randomIndex);

            //if region is enabled and hasnt already been chosen
            if (!quizCountriesList.contains(fileName)) {
                quizCountriesList.add(fileName);
                ++flagCounter;

            }

        }//while
        loadNextFlag(); //start the quiz by loading 1st flag
    }
        // after the user guesses a correct flag, load the next flag

    private void loadNextFlag() {
        // get file name of the next flag and remove it from the list
        String nextImageName = quizCountriesList.remove(0);

        correctAnswer = nextImageName; // update the correct answer

        answerTextView.setText(""); // clear answerTextView

        // display the number of the current question in the quiz
        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " " +
                        (correctAnswers + 1) + " " +
                        getResources().getString(R.string.of) + " 10");

        // extract the region from the next image's name
        String region =
                nextImageName.substring(0, nextImageName.indexOf('-'));

        // use AssetManager to load next image from assets folder
        AssetManager assets = getAssets(); // get app's AssetManager
        InputStream stream; // used to read in flag images

        try {
            // get an InputStream to the asset representing the next flag
            stream = assets.open(region + "/" + nextImageName + ".png");

            // load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImageName);
            flagImageView.setImageDrawable(flag);
        } // end try
        catch (IOException e) {
            Log.e(TAG, "Error loading " + nextImageName, e);
        } // end catch

        // clear prior answer Buttons from TableRows
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

        Collections.shuffle(fileNameList); // shuffle file names


        // put the correct answer at the end of fileNameList later will be inserted randomly into the answer Buttons
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // get a reference to the LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // add 3, 6, or 9 answer Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            TableRow currentTableRow = getTableRow(row); //obtains the TableRow at a specific index in the buttonTableLayout

            // place Buttons in currentTableRow
            for (int column = 0; column < 3; column++) {
                // inflate guess_button.xml to create new Button
                Button newGuessButton =
                        (Button) inflater.inflate(R.layout.guess_button, null);

                // get country name and set it as newGuessButton's text
                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getCountryName(fileName));

                // register answerButtonListener to respond to button clicks
                newGuessButton.setOnClickListener(guessButtonListener);
                currentTableRow.addView(newGuessButton);
            } // end for
        } // end for

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(3); // pick random column
        TableRow randomTableRow = getTableRow(row); // get the TableRow
        String countryName = getCountryName(correctAnswer);
        ((Button) randomTableRow.getChildAt(column)).setText(countryName);
    } // end method loadNextFlag

    // returns the specified TableRow
    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    } // end method getTableRow

    // parses the country flag file name and returns the country name
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    } // end method getCountryName



    // called when the user selects an answer
    private void submitGuess(Button guessButton)
    {
        String guess = guessButton.getText().toString();
        String answer = getCountryName(correctAnswer);
        ++totalGuesses; // increment the number of guesses the user has made

        // if the guess is correct
        if (guess.equals(answer))
        {
            ++correctAnswers; // increment the number of correct answers

            // display "Correct!" in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.correct_answer));

            disableButtons(); // disable all answer Buttons

            // if the user has correctly identified 10 flags
            if (correctAnswers == 10)
            {
                // create a new AlertDialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.reset_quiz); // title bar string

                // set the AlertDialog's message to display game results
                builder.setMessage(String.format("%d %s, %.02f%% %s",
                        totalGuesses, getResources().getString(R.string.guesses),
                        (1000 / (double) totalGuesses),
                        getResources().getString(R.string.correct)));

                builder.setCancelable(false);

                // add "Reset Quiz" Button
                builder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                resetQuiz();
                            } // end method onClick
                        } // end anonymous inner class
                ); // end call to setPositiveButton

                // create AlertDialog from the Builder
                AlertDialog resetDialog = builder.create();
                resetDialog.show(); // display the Dialog
            } // end if
            else // answer is correct but quiz is not over
            {
                // load the next flag after a 1-second delay
                handler.postDelayed(
                        //anonymous inner class implementing Runnable
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                loadNextFlag();
                            }
                        }, 1000); // 1000 milliseconds for 1-second delay
            } // end else
        } // end if
        else // guess was incorrect
        {
            // play the animation
            flagImageView.startAnimation(shakeAnimation);

            // display "Incorrect!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); // disable the incorrect answer
        } // end else
    } // end method submitGuess

    // utility method that disables all answer Buttons
    private void disableButtons()
    {
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
        {
            TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
            for (int i = 0; i < tableRow.getChildCount(); ++i)
                tableRow.getChildAt(i).setEnabled(false);
        } // end outer for
    } // end method disableButtons

    //called when a guess button is touched
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            submitGuess((Button)v); //pass seslected button to submitGuess

        }
    };
}
