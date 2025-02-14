package owlmoney.model.goals;

import owlmoney.model.bank.Bank;
import owlmoney.model.goals.exception.GoalsException;
import owlmoney.storage.Storage;
import owlmoney.ui.Ui;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import static owlmoney.commons.log.LogsCenter.getLogger;

/**
 * Contains the list of goals in the profile.
 */
public class GoalsList {
    private ArrayList<Goals> goalList;
    private static final int ONE_INDEX = 1;
    private static final boolean ISMULTIPLE = true;
    private static final boolean ISSINGLE = false;
    private static final int ISZERO = 0;
    private Storage storage;
    private static final String PROFILE_GOAL_LIST_FILE_NAME = "profile_goallist.csv";
    private static final String UNTIEDBANK = "-NOT TIED-";
    private static final Logger logger = getLogger(GoalsList.class);

    /**
     * Creates a instance of GoalsList that contains an arrayList of Goals.
     *
     * @param storage for importing and exporting purposes.
     */
    public GoalsList(Storage storage) {
        goalList = new ArrayList<Goals>();
        this.storage = storage;
    }

    /**
     * Limits the number of goals that user can have when setting goals.
     *
     * @throws GoalsException If number of goals exceeds 20.
     */
    private void checkNumGoals() throws GoalsException {
        if (goalList.size() >= 20) {
            logger.warning("Exceeded limit of having 20 goals");
            throw new GoalsException("You've reached the limit of 20 goals!");
        }
    }

    /**
     * Lists all goals in GoalsList.
     *
     * @param ui required for printing.
     */
    public void listGoals(Ui ui) {
        if (goalList.size() <= ISZERO) {
            ui.printError("There are no goals set");
            logger.warning("Trying to list empty goals");
        } else {
            ui.printGoalHeader();
            for (int i = ISZERO; i < goalList.size(); i++) {
                printOneGoal((i + ONE_INDEX), goalList.get(i), ISMULTIPLE, ui);
            }
            ui.printGoalDivider();
            logger.info("Succeed in listing all goals in list");
            try {
                exportGoalList();
            } catch (IOException e) {
                ui.printError("Error trying to save your goals to disk. Your data is"
                        + " at risk, but we will try again, feel free to continue using the program.");
                logger.warning("Failed to save data during /list /goals");
            }
        }
    }

    /**
     * Adds an instance of goals into GoalsList.
     *
     * @param goals a new goal object.
     * @param ui    required for printing.
     * @throws GoalsException If a duplicate goal name is found.
     */
    public void addToGoals(Goals goals, Ui ui) throws GoalsException {
        if (goalExists(goals.getGoalsName())) {
            logger.warning("New name already exists in list");
            throw new GoalsException("There is already a goal with the same name " + goals.getGoalsName());
        }
        if (goals.getRawStatus()) {
            logger.warning("Attempted to add a goal with lesser amount then balance of saving account");
            throw new GoalsException("You cannot add a goal that is already achieved!");
        }
        checkNumGoals();
        goalList.add(goals);
        try {
            exportGoalList();
        } catch (IOException e) {
            ui.printError("Error trying to save your goals to disk. Your data is"
                    + " at risk, but we will try again, feel free to continue using the program.");
            logger.warning("Failed to save data during /add /goals");
        }
        ui.printMessage("Added a new goal with the below details: ");
        printOneGoal(ONE_INDEX, goals, ISSINGLE, ui);
        logger.info("Successfully added a new goal");
    }

    /**
     * Deletes a goal from GoalsList.
     *
     * @param goalName The name of the goal.
     * @param ui       required for printing.
     * @throws GoalsException If trying to delete from empty GoalsList
     */
    public void deleteFromGoalList(String goalName, Ui ui) throws GoalsException {
        if (goalList.size() <= ISZERO) {
            logger.warning("Goal list is empty");
            throw new GoalsException("There are no goals with the name: " + goalName);
        } else {
            String capitalGoalName = goalName.toUpperCase();
            for (int i = ISZERO; i < goalList.size(); i++) {
                Goals currentGoal = goalList.get(i);
                String currentGoalName = currentGoal.getGoalsName();
                String capitalCurrentGoalName = currentGoalName.toUpperCase();
                if (capitalGoalName.equals(capitalCurrentGoalName)) {
                    Goals temp = goalList.get(i);
                    goalList.remove(i);
                    ui.printMessage("Details of the goal being removed:");
                    printOneGoal(ONE_INDEX, temp, ISSINGLE, ui);
                    logger.info("Successfully deleted goal : " + goalName);
                    try {
                        exportGoalList();
                    } catch (IOException e) {
                        ui.printError("Error trying to save your goals to disk. Your data is"
                                + " at risk, but we will try again, feel free to continue using the program.");
                        logger.warning("Failed to save data during /delete /goals");
                    }
                    return;
                }
            }
            logger.warning("Name don't exist when trying to delete a goal");
            throw new GoalsException("There are no goals with the name: " + goalName);
        }
    }

    /**
     * Compares there is already a goal name that exists.
     *
     *
     * @param currentGoal Name of current goal.
     * @param newGoalName New Goal Name that user intends to change.
     * @throws GoalsException If there's a goal of the same name.
     */
    private void compareGoals(Goals currentGoal, String newGoalName) throws GoalsException {
        String currentGoalName = currentGoal.getGoalsName();
        String capitalCurrentGoalName = currentGoalName.toUpperCase();
        String capitalNewGoalName = newGoalName.toUpperCase();

        for (int i = ISZERO; i < goalList.size(); i++) {
            Goals checkGoal = goalList.get(i);
            String checkGoalName = checkGoal.getGoalsName();
            String capitalCheckGoalName = checkGoalName.toUpperCase();
            if (capitalCheckGoalName.equals(capitalNewGoalName) && checkGoalName.equals(newGoalName)
                    && !currentGoalName.equals(newGoalName) && !capitalCurrentGoalName.equals(capitalNewGoalName)) {
                logger.warning("Name you've chosen is already in the goals list");
                throw new GoalsException("There is already a goal with the same name: " + newGoalName);
            }
        }
    }

    /**
     * Add savings account to un-tracked goals if true. If false, remove savings account from tracked goals.
     *
     * @param currentGoal Goal object to get savings account detail.
     * @param savingAcc Name of savings account to link / unlinked from goals.
     *
     * @return true /false if to add / remove savings account from un-tracked or tracked goals.
     */
    private boolean compareGoalSavingAcc(Goals currentGoal, Bank savingAcc) {
        if (!currentGoal.getSavingAccount().equals("-NOT TIED-")
                && currentGoal.getSavingAccount().equals(savingAcc.getAccountName())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a goal exists when wanting to add a new goal.
     *
     * @param goalName refers to the about-to-add goal name.
     * @return True if it exists and False if it doesn't.
     */
    private boolean goalExists(String goalName) {
        String capitalGoalName = goalName.toUpperCase();
        for (int i = ISZERO; i < goalList.size(); i++) {
            Goals currentGoal = goalList.get(i);
            String currentGoalName = currentGoal.getGoalsName();
            String capitalCurrentGoalName = currentGoalName.toUpperCase();
            if (capitalGoalName.equals(capitalCurrentGoalName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Edits an instance of the goal.
     *
     * @param goalName To identify and retrieve the goal details.
     * @param amount   New amount of the goal.
     * @param date     New date to reach the goal.
     * @param newName  New name to identify the goal.
     * @param ui       required for printing.
     * @throws GoalsException If date is not in correct format, or changing to a name that already exists,
     *                        or no goal with the goalName.
     */
    public void editGoals(String goalName, String amount, Date date, String newName, Bank savingAcc,
                          boolean markDone, Ui ui) throws GoalsException {
        String capitalGoalName = goalName.toUpperCase();
        for (int i = ISZERO; i < goalList.size(); i++) {
            Goals currentGoal = goalList.get(i);
            String currentGoalName = currentGoal.getGoalsName();
            String capitalCurrentGoalName = currentGoalName.toUpperCase();
            if (capitalGoalName.equals(capitalCurrentGoalName)) {
                if (currentGoal.getRawStatus()) {
                    logger.warning("Tried to edit an already achieved goal.");
                    throw new GoalsException("Sorry, you cannot edit a goal that's already achieved! "
                            + "Try creating a new goal instead!");
                }
                if (!(newName == null || newName.isBlank())) {
                    compareGoals(currentGoal, newName);
                    currentGoal.setGoalsName(newName);
                }
                if (!(amount == null || amount.isBlank())) {
                    if (!currentGoal.getSavingAccount().equals("-NOT TIED-")
                            && currentGoal.getSavingAmount() <= currentGoal.getGoalsAmount()) {
                        logger.warning("Attempted to edit a goal with lesser / same amount "
                                + "then balance of saving account");
                        throw new GoalsException("You cannot edit a goal with equal / less amount as "
                                + "savings account balance!");
                    } else {
                        currentGoal.setGoalsAmount(Double.parseDouble(amount));
                    }
                }
                if (date != null) {
                    currentGoal.setGoalsDate(date);
                }
                if (savingAcc != null) {
                    if (compareGoalSavingAcc(currentGoal, savingAcc)) {
                        currentGoal.setSavingAccount(null);
                    } else if (savingAcc.getCurrentAmount() < currentGoal.getGoalsAmount()) {
                        currentGoal.setSavingAccount(savingAcc);
                    } else {
                        logger.warning("Attempted to add a goal with lesser amount "
                                + "then balance of saving account");
                        throw new GoalsException("You cannot add a goal that is already achieved!");
                    }
                }
                if (markDone) {
                    if (currentGoal.savingAccNotTied()) {
                        currentGoal.markDone();
                    } else {
                        logger.warning("Tried to mark done a tracked goal");
                        throw new GoalsException("You cannot mark a goal that is linked to a saving account!");
                    }
                }
                try {
                    exportGoalList();
                } catch (IOException e) {
                    ui.printError("Error trying to save your goals to disk. Your data is"
                            + " at risk, but we will try again, feel free to continue using the program.");
                    logger.warning("Failed to save data during /edit /goals");
                }
                ui.printMessage("New details of goals changed: ");
                printOneGoal(ONE_INDEX, goalList.get(i), ISSINGLE, ui);
                logger.info("Successfully changed details of goals");
                return;
            }
        }
        logger.warning("Name don't exist when trying to delete a goal");
        throw new GoalsException("There are no goals with the name: " + goalName);
    }

    /**
     * Prints goal details.
     *
     * @param num                Represents the numbering of the goal.
     * @param goal               The goal object to be printed.
     * @param isMultiplePrinting Represents whether the function will be called for printing once or multiple
     *                           time
     * @param ui                 The object use for printing.
     */
    private void printOneGoal(int num, Goals goal, boolean isMultiplePrinting, Ui ui) {
        if (!isMultiplePrinting) {
            ui.printGoalHeader();
        }
        if (!goal.getSavingAccount().isBlank()) {
            goal.isDone(Double.parseDouble(goal.getRemainingAmount()));
        }
        ui.printGoal(num, goal.getGoalsName(), "$"
                        + new DecimalFormat("0.00").format(goal.getGoalsAmount()), goal.getSavingAccount(),
                "$" + goal.getRemainingAmount(), goal.getGoalsDate(), goal.getStatus());
        if (!isMultiplePrinting) {
            ui.printGoalDivider();
        }
    }

    /**
     * Updates all goals in the list.
     */
    public void updateGoals() {
        for (int i = 0; i < goalList.size(); i++) {
            goalList.get(i).isDone(Double.parseDouble(goalList.get(i).getRemainingAmount()));
        }
    }

    /**
     * Change all goals tied to a deleted account to untied.
     *
     * @param bankName Name of deleted bank account.
     */
    public void changeTiedAccountsToNull(String bankName) {
        String capitalBankName = bankName.toUpperCase();
        for (int i = ISZERO; i < goalList.size(); i++) {
            Goals currentGoal = goalList.get(i);
            String currentGoalBank = currentGoal.getSavingAccount();
            if (currentGoalBank == null) {
                continue;
            }
            String capitalCurrentGoalBank = currentGoalBank.toUpperCase();
            if (capitalBankName.equals(capitalCurrentGoalBank)) {
                currentGoal.setSavingAccount(null);
            }
        }
    }

    /**
     * Gets the size of the goalList which counts all the goals stored in the ArrayList of goals.
     *
     * @return size of goalList.
     */
    public int getGoalListSize() {
        return goalList.size();
    }

    /**
     * Prepares the goalList for exporting of attributes of each goal.
     *
     * @return ArrayList of String arrays for containing each bank in the bank list.
     */
    private ArrayList<String[]> prepareExportGoalList() {
        ArrayList<String[]> exportArrayList = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        SimpleDateFormat exportDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        exportArrayList.add(new String[]{
            "goalName", "amount", "date", "savingsAccountName", "doneStatus", "achieveStatus"});
        for (int i = 0; i < getGoalListSize(); i++) {
            String goalName = goalList.get(i).getGoalsName();
            double amount = goalList.get(i).getGoalsAmount();
            String stringAmount = decimalFormat.format(amount);
            String date = exportDateFormat.format(goalList.get(i).getGoalsDateInDateFormat());
            String savingsAccountName = goalList.get(i).getSavingAccount();
            if (UNTIEDBANK.equals(savingsAccountName)) {
                savingsAccountName = null;
            }
            boolean doneStatus = goalList.get(i).getRawStatus();
            String stringDoneStatus = String.valueOf(doneStatus);
            boolean achievementStatus = goalList.get(i).getGoalAchievementStatus();
            String achievementStatusString = String.valueOf(achievementStatus);
            exportArrayList.add(new String[]{
                goalName, stringAmount, date, savingsAccountName, stringDoneStatus, achievementStatusString});
        }
        logger.info("Successfully added all goals into the arraylist");
        return exportArrayList;
    }

    /**
     * Writes the data of the bank list that was prepared to permanent storage.
     *
     * @throws IOException when unable to write to file.
     */
    private void exportGoalList() throws IOException {
        ArrayList<String[]> inputData = prepareExportGoalList();
        storage.writeFile(inputData, PROFILE_GOAL_LIST_FILE_NAME);
        logger.info("Successfully exported goals");
    }

    /**
     * Imports goals loaded from save file into goalList.
     *
     * @param newGoal an instance of the goal to be imported.
     */
    public void goalListImportNewGoal(Goals newGoal) {
        goalList.add(newGoal);
        logger.info("Successfully imported goals");
    }

    /**
     * Checks if goals can get achievement.
     *
     * @param i  index of goals.
     * @param ui Required for Printing.
     * @return Achievement object to create new achievement.
     */
    public Achievement checkForAchievement(int i, Ui ui) {
        Goals checkAchievement = goalList.get(i);
        if (checkAchievement.getRawStatus() && checkAchievement.getGoalsDateInDateFormat().after(new Date())
                && !checkAchievement.getGoalAchievementStatus()) {
            Achievement unlocked = new Achievement(checkAchievement.getGoalsName(), checkAchievement.getGoalsAmount(),
                    "[GOALS]", checkAchievement.getGoalsDateInDateFormat());
            checkAchievement.achieveGoal();
            try {
                exportGoalList();
            } catch (IOException e) {
                ui.printError("Error trying to save your goals to disk. Your data is"
                        + " at risk, but we will try again, feel free to continue using the program.");
                logger.warning("Error exporting achievement status");
            }
            return unlocked;
        }
        try {
            exportGoalList();
        } catch (IOException e) {
            ui.printError("Error trying to save your goals to disk. Your data is"
                    + " at risk, but we will try again, feel free to continue using the program.");
            logger.warning("Error exporting achievement status");
        }
        return null;
    }

    /**
     * Check goals that is due in 10 days.
     */
    public void reminderForGoals(Ui ui) {
        int count = 0;
        if (goalList.size() <= ISZERO) {
            ui.printMessage("NO REMINDER FOR GOALS");
        } else {
            ui.printMessage("\nREMINDER FOR GOALS: ");
            for (int i = 0; i < goalList.size(); i++) {
                if (goalList.get(i).convertDateToDays() == 0 && !goalList.get(i).getRawStatus()
                        && goalList.get(i).getGoalsDateInDateFormat().after(new Date())) {
                    ui.printMessage("- " + goalList.get(i).getGoalsName() + " is due in 1 day"
                            + "\n(You still have a remaining of $" + goalList.get(i).getRemainingAmount()
                            + " to reach your goal!)");
                    count++;
                } else if (goalList.get(i).getGoalsDateInDateFormat().after(new Date())
                        && goalList.get(i).convertDateToDays() <= 10 && !goalList.get(i).getRawStatus()) {
                    ui.printMessage("- " + goalList.get(i).getGoalsName() + " is due in "
                            + goalList.get(i).convertDateToDays() + " days. " + "\n(You still have a remaining of $"
                            + goalList.get(i).getRemainingAmount() + " to reach your goal!)");
                    count++;
                }
            }
            if (count == 0) {
                ui.printMessage("NO REMINDER FOR GOALS");
            }
        }
    }

    /**
     * Check goals that is overdue.
     */
    public void overdueGoals(Ui ui) {
        int count = 0;
        if (goalList.size() <= ISZERO) {
            ui.printMessage("NO OVERDUE FOR GOALS");
        } else {
            ui.printMessage("\nOVERDUE GOALS: ");
            for (int i = 0; i < goalList.size(); i++) {
                if (!goalList.get(i).getRawStatus() && goalList.get(i).getGoalsDateInDateFormat().before(new Date())) {
                    ui.printMessage("- " + goalList.get(i).getGoalsName()
                            + " to save $" + goalList.get(i).getRemainingAmount() + " is overdue!");
                    count++;
                }
            }
            if (count == 0) {
                ui.printMessage("NO OVERDUE GOALS");
            }
        }
    }
}
